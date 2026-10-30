#!/usr/bin/env python3
"""
AEM DAM migration helper (folder-creation fix).

Default: DRY_RUN=True; no destination writes occur.
- Reads assets under SOURCE_FOLDER using AEM QueryBuilder.
- Downloads each original rendition.
- Creates destination parent folders in order only in live mode.
- Skips destination assets that already exist.
- Attempts best-effort scalar metadata transfer via Sling POST.
- Writes a log and CSV report.

Important:
1) Test with a small non-production folder first.
2) Empty source folders are not migrated by this version; folders containing assets
   are reconstructed from each asset's relative parent path.
3) Metadata upload behavior can vary by AEM version and configuration.
"""

from __future__ import annotations

import csv
import getpass
import logging
import mimetypes
import sys
import tempfile
import time
from pathlib import Path, PurePosixPath
from typing import Any
from urllib.parse import quote

import requests
from requests.auth import HTTPBasicAuth

# --------------------------- Configuration ---------------------------
SOURCE_BASE = "http://localhost:4502"
SOURCE_FOLDER = "/content/dam/multisite/press-releases-images"

DEST_BASE = "http://192.168.1.82:4502"
DEST_PARENT = "/content/dam/multisite"
DEST_FOLDER_NAME = "press-releases-images-migrated"
DEST_FOLDER = f"{DEST_PARENT}/{DEST_FOLDER_NAME}"

SOURCE_USERNAME = "admin"
DEST_USERNAME = "admin"

# Keep True for a read-only scan. Change to False only after reviewing the report.
DRY_RUN = False
SKIP_EXISTING = True
PAGE_SIZE = 200
CONNECT_TIMEOUT = 10
READ_TIMEOUT = 120

OUTPUT_DIR = Path("aem_dam_migration_logs")
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
RUN_ID = time.strftime("%Y%m%d_%H%M%S")
LOG_FILE = OUTPUT_DIR / f"migration_{RUN_ID}.log"
REPORT_FILE = OUTPUT_DIR / f"migration_report_{RUN_ID}.csv"
TIMEOUT = (CONNECT_TIMEOUT, READ_TIMEOUT)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    handlers=[
        logging.FileHandler(LOG_FILE, encoding="utf-8"),
        logging.StreamHandler(sys.stdout),
    ],
)
log = logging.getLogger("aem-dam-migration")


def norm_path(path: str) -> str:
    p = PurePosixPath("/" + path.lstrip("/"))
    if ".." in p.parts:
        raise ValueError(f"Unsafe repository path: {path}")
    return str(p)


def encoded_path(path: str) -> str:
    return quote(norm_path(path), safe="/")


def make_session(username: str, label: str) -> requests.Session:
    password = getpass.getpass(f"{label} AEM password for {username}: ")
    session = requests.Session()
    session.auth = HTTPBasicAuth(username, password)
    session.headers.update({"User-Agent": "AEM-DAM-Migration-Python/1.1"})
    return session


def raise_for_status(response: requests.Response, context: str) -> None:
    if response.status_code in (401, 403):
        raise RuntimeError(
            f"{context}: HTTP {response.status_code}; verify credentials and AEM permissions."
        )
    response.raise_for_status()


def check_host(session: requests.Session, base: str, label: str) -> None:
    url = f"{base.rstrip('/')}/libs/granite/core/content/login.html"
    try:
        response = session.get(url, timeout=TIMEOUT, allow_redirects=True)
    except requests.RequestException as exc:
        raise RuntimeError(f"{label} AEM unreachable at {base}: {exc}") from exc
    if response.status_code in (401, 403):
        raise RuntimeError(f"{label} AEM responded HTTP {response.status_code}; check login/access.")
    if response.status_code >= 500:
        raise RuntimeError(f"{label} AEM returned HTTP {response.status_code}.")
    log.info("%s host reachable (HTTP %s): %s", label, response.status_code, base)


def query_assets(session: requests.Session, base: str, root: str) -> list[str]:
    """Discover only dam:Asset nodes, not arbitrary internal folders below asset nodes."""
    endpoint = f"{base.rstrip('/')}/bin/querybuilder.json"
    found: list[str] = []
    offset = 0
    while True:
        params = {
            "path": root,
            "type": "dam:Asset",
            "p.limit": str(PAGE_SIZE),
            "p.offset": str(offset),
            "p.hits": "full",
            "p.properties": "jcr:path",
            "orderby": "@jcr:path",
            "orderby.sort": "asc",
        }
        response = session.get(endpoint, params=params, timeout=TIMEOUT)
        raise_for_status(response, "QueryBuilder asset discovery")
        try:
            payload = response.json()
        except ValueError as exc:
            raise RuntimeError("QueryBuilder response was not JSON; check endpoint/version.") from exc
        hits = payload.get("hits", [])
        if not isinstance(hits, list):
            raise RuntimeError("Unexpected QueryBuilder JSON: 'hits' is not a list.")
        for hit in hits:
            path = hit.get("jcr:path")
            if path:
                path = norm_path(path)
                # Only direct asset paths or descendants in the source subtree.
                if path.startswith(root.rstrip("/") + "/"):
                    found.append(path)
        if len(hits) < PAGE_SIZE:
            break
        offset += len(hits)
    return sorted(set(found))


def get_metadata(session: requests.Session, base: str, asset_path: str) -> dict[str, Any]:
    url = f"{base.rstrip('/')}{encoded_path(asset_path)}/jcr:content/metadata.json"
    response = session.get(url, timeout=TIMEOUT)
    if response.status_code == 404:
        log.warning("Metadata JSON unavailable for %s; continuing without metadata", asset_path)
        return {}
    raise_for_status(response, f"Read metadata for {asset_path}")
    try:
        raw = response.json()
    except ValueError:
        log.warning("Metadata response was not JSON for %s", asset_path)
        return {}

    excluded = {
        "jcr:primaryType", "jcr:mixinTypes", "jcr:created", "jcr:createdBy",
        "jcr:lastModified", "jcr:lastModifiedBy", "cq:lastReplicated",
        "cq:lastReplicatedBy", "cq:lastReplicationAction", "dam:sha1",
        "dam:size", "dam:status", "dam:extracted",
    }
    result: dict[str, Any] = {}
    for key, value in raw.items():
        if key in excluded or key.startswith(("jcr:", "cq:")):
            continue
        if isinstance(value, (str, int, float, bool)):
            result[key] = value
        elif isinstance(value, list) and all(
            isinstance(item, (str, int, float, bool)) for item in value
        ):
            result[key] = value
    return result


def download_original(
    session: requests.Session, base: str, asset_path: str, local_path: Path
) -> None:
    url = f"{base.rstrip('/')}{encoded_path(asset_path)}/jcr:content/renditions/original"
    with session.get(url, stream=True, timeout=TIMEOUT) as response:
        raise_for_status(response, f"Download original rendition {asset_path}")
        with local_path.open("wb") as out:
            for chunk in response.iter_content(chunk_size=1024 * 1024):
                if chunk:
                    out.write(chunk)


def path_exists(session: requests.Session, base: str, repo_path: str) -> bool:
    url = f"{base.rstrip('/')}{encoded_path(repo_path)}.json"
    response = session.get(url, timeout=TIMEOUT)
    if response.status_code == 404:
        return False
    if response.status_code in (401, 403):
        raise RuntimeError(
            f"Cannot read/check {repo_path}: HTTP {response.status_code}; check permissions."
        )
    if response.status_code >= 500:
        response.raise_for_status()
    return response.status_code == 200


def create_folder(session: requests.Session, base: str, folder_path: str) -> None:
    """
    Create a Sling folder by POSTing to the target resource path itself.
    The parent must already exist. This avoids the previous behavior where
    posting :name to the parent returned 'Content modified /content/dam'
    without reliably creating the requested child folder.
    """
    folder_path = norm_path(folder_path)
    parent = str(PurePosixPath(folder_path).parent)

    if not path_exists(session, base, parent):
        raise RuntimeError(
            f"Cannot create {folder_path}: parent folder {parent} does not exist "
            "or is not readable. Parent folders must be created first."
        )

    # POST to the exact target path (not the parent + :name).
    url = f"{base.rstrip('/')}{encoded_path(folder_path)}"
    response = session.post(
        url,
        data={"jcr:primaryType": "sling:Folder"},
        timeout=TIMEOUT,
        allow_redirects=False,
    )

    # If AEM says conflict, accept only when a read-only check confirms the node exists.
    if response.status_code in (409, 412):
        if path_exists(session, base, folder_path):
            log.info("Folder already exists (verified after conflict): %s", folder_path)
            return
        raise RuntimeError(
            f"AEM returned HTTP {response.status_code} creating {folder_path}, "
            "but the folder could not be verified."
        )

    if response.status_code not in (200, 201, 204, 302):
        raise_for_status(response, f"Create folder {folder_path}")
        raise RuntimeError(
            f"Unexpected HTTP {response.status_code} creating {folder_path}: "
            f"{response.text[:500]}"
        )

    # Sling may return 200 with a 'Content modified <parent>' page. That is not
    # sufficient proof of child creation, so verify the exact target resource.
    if not path_exists(session, base, folder_path):
        location = response.headers.get("Location", "(no Location header)")
        body = response.text[:500].replace("\\n", " ")
        raise RuntimeError(
            f"AEM returned HTTP {response.status_code}, but folder verification failed "
            f"for {folder_path}. Location={location}. Response: {body}. "
            "Check the destination AEM error log and write permissions."
        )
    log.info("Created and verified folder: %s", folder_path)


def ensure_destination_folders(
    session: requests.Session, base: str, folders: set[str]
) -> None:
    # Explicitly attempt the multisite folder creation without an existence pre-check.
    # If it already exists, create_folder handles the conflict by verifying the path.
    multisite_path = "/content/dam/multisite"
    ordered = sorted(folders, key=lambda p: (p.count("/"), p))
    if multisite_path in ordered:
        ordered.remove(multisite_path)
        # Parent /content/dam must exist for Sling POST to create /content/dam/multisite.
        ordered.insert(0, multisite_path)

    for folder in ordered:
        if DRY_RUN:
            log.info("[DRY RUN] Would attempt folder creation: %s", folder)
            continue
        if folder == multisite_path:
            # No pre-check of /content/dam/multisite. The POST is attempted directly.
            create_folder(session, base, folder)
            continue
        # For other folders, check first to avoid unnecessary conflicts.
        if path_exists(session, base, folder):
            log.info("Destination folder already exists: %s", folder)
            continue
        create_folder(session, base, folder)


def upload_asset(
    session: requests.Session,
    base: str,
    target_parent: str,
    local_file: Path,
    filename: str,
    mime_type: str,
    metadata: dict[str, Any],
) -> None:
    url = f"{base.rstrip('/')}{encoded_path(target_parent)}"
    form: dict[str, str] = {}
    for key, value in metadata.items():
        if not key or key.startswith(":") or "/" in key or "\\" in key:
            continue
        if isinstance(value, list):
            # Sling POST multi-value syntax. Verify custom metadata after upload.
            form[f"./jcr:content/metadata/{key}@TypeHint"] = "String[]"
            for index, item in enumerate(value):
                form[f"./jcr:content/metadata/{key}/{index}"] = str(item)
        else:
            form[f"./jcr:content/metadata/{key}"] = str(value)

    with local_file.open("rb") as fh:
        response = session.post(
            url,
            data=form,
            files={"file": (filename, fh, mime_type or "application/octet-stream")},
            timeout=TIMEOUT,
            allow_redirects=False,
        )
    if response.status_code not in (200, 201, 204, 302):
        raise_for_status(response, f"Upload {target_parent}/{filename}")
        raise RuntimeError(f"Unexpected HTTP {response.status_code} uploading {filename}")

    target_asset = f"{target_parent}/{filename}"
    if not path_exists(session, base, target_asset):
        raise RuntimeError(
            f"Upload returned HTTP {response.status_code}, but destination asset was not found: "
            f"{target_asset}. Check AEM upload endpoint and permissions."
        )
    log.info("Uploaded and verified asset: %s", target_asset)


def write_report(rows: list[dict[str, str]]) -> None:
    with REPORT_FILE.open("w", newline="", encoding="utf-8-sig") as fh:
        writer = csv.DictWriter(
            fh, fieldnames=["status", "source_path", "destination_path", "details"]
        )
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    print("AEM DAM migration")
    print(f"Source:      {SOURCE_BASE}{SOURCE_FOLDER}")
    print(f"Destination: {DEST_BASE}{DEST_FOLDER}")
    print(f"Mode:        {'DRY RUN (no writes)' if DRY_RUN else 'LIVE MIGRATION'}")
    print("Passwords are requested interactively and are not stored in this script.\n")

    source = make_session(SOURCE_USERNAME, "Source")
    dest = make_session(DEST_USERNAME, "Destination")
    rows: list[dict[str, str]] = []

    try:
        check_host(source, SOURCE_BASE, "Source")
        check_host(dest, DEST_BASE, "Destination")

        if not path_exists(source, SOURCE_BASE, SOURCE_FOLDER):
            raise RuntimeError(
                f"Source folder not found/readable: {SOURCE_FOLDER}. Check path and permissions."
            )

        assets = query_assets(source, SOURCE_BASE, SOURCE_FOLDER)
        if not assets:
            raise RuntimeError(
                "No dam:Asset results found. Check source path, permissions and QueryBuilder support."
            )
        log.info("Discovered %d assets", len(assets))

        # Build only real destination folder paths from asset parent paths.
        # Do NOT query all sling:Folder nodes under the source subtree: that can return
        # internal asset nodes such as img.jpg/jcr:content/renditions.
        folders_to_create: set[str] = set()
        for repo_folder in ("/content/dam", DEST_PARENT, DEST_FOLDER):
            p = PurePosixPath(repo_folder)
            ancestors = [str(parent) for parent in reversed(p.parents) if str(parent) != "/"]
            folders_to_create.update(ancestors)
            folders_to_create.add(str(p))

        for asset_path in assets:
            relative_parent = PurePosixPath(asset_path).parent.relative_to(SOURCE_FOLDER)
            target_parent = DEST_FOLDER if str(relative_parent) == "." else (
                f"{DEST_FOLDER}/{relative_parent.as_posix()}"
            )
            p = PurePosixPath(target_parent)
            folders_to_create.update(
                str(parent) for parent in reversed(p.parents)
                if str(parent) not in ("/", "/content")
            )
            folders_to_create.add(target_parent)

        # Remove paths above /content/dam; never try to create /content or /.
        folders_to_create = {
            p for p in folders_to_create
            if p == "/content/dam" or p.startswith("/content/dam/")
        }
        ensure_destination_folders(dest, DEST_BASE, folders_to_create)

        with tempfile.TemporaryDirectory(prefix="aem_dam_migration_") as tmp:
            temp_root = Path(tmp)
            for idx, asset_path in enumerate(assets, start=1):
                rel = PurePosixPath(asset_path).relative_to(SOURCE_FOLDER)
                target_asset = f"{DEST_FOLDER}/{rel.as_posix()}"
                target_parent = str(PurePosixPath(target_asset).parent)
                filename = PurePosixPath(asset_path).name

                try:
                    if SKIP_EXISTING and path_exists(dest, DEST_BASE, target_asset):
                        log.info("SKIP existing asset: %s", target_asset)
                        rows.append({
                            "status": "SKIPPED_EXISTS",
                            "source_path": asset_path,
                            "destination_path": target_asset,
                            "details": "Existing destination asset left unchanged",
                        })
                        continue

                    metadata = get_metadata(source, SOURCE_BASE, asset_path)
                    local_file = temp_root / f"asset_{idx}_{filename}"
                    download_original(source, SOURCE_BASE, asset_path, local_file)
                    size = local_file.stat().st_size
                    mime_type = mimetypes.guess_type(filename)[0] or "application/octet-stream"

                    if DRY_RUN:
                        log.info(
                            "[DRY RUN] Would upload %s -> %s (%.2f MB; %d metadata fields)",
                            asset_path, target_asset, size / (1024 * 1024), len(metadata)
                        )
                        rows.append({
                            "status": "DRY_RUN_READY",
                            "source_path": asset_path,
                            "destination_path": target_asset,
                            "details": f"bytes={size}; metadata_fields={len(metadata)}",
                        })
                    else:
                        if not path_exists(dest, DEST_BASE, target_parent):
                            raise RuntimeError(
                                f"Target parent folder missing after creation phase: {target_parent}"
                            )
                        upload_asset(
                            dest, DEST_BASE, target_parent, local_file,
                            filename, mime_type, metadata
                        )
                        rows.append({
                            "status": "UPLOADED",
                            "source_path": asset_path,
                            "destination_path": target_asset,
                            "details": f"bytes={size}; metadata_fields_sent={len(metadata)}",
                        })
                except Exception as exc:
                    log.exception("Failed asset: %s", asset_path)
                    rows.append({
                        "status": "FAILED",
                        "source_path": asset_path,
                        "destination_path": target_asset,
                        "details": str(exc),
                    })

        write_report(rows)
        summary: dict[str, int] = {}
        for row in rows:
            summary[row["status"]] = summary.get(row["status"], 0) + 1
        log.info("Finished. Summary: %s", summary)
        log.info("Log file: %s", LOG_FILE.resolve())
        log.info("CSV report: %s", REPORT_FILE.resolve())
        return 1 if any(row["status"] == "FAILED" for row in rows) else 0

    except Exception as exc:
        log.exception("Migration stopped: %s", exc)
        if rows:
            write_report(rows)
        log.error("Review log: %s", LOG_FILE.resolve())
        return 2
    finally:
        source.close()
        dest.close()


if __name__ == "__main__":
    raise SystemExit(main())
