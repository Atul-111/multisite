#!/usr/bin/env python3
"""
Press Release Content Fragments auto-creator (AEM 6.5.22)

Ye script:
 1. Model ke field names verify karti hai
 2. Folder banati hai (agar nahi hai)
 3. Images dhoondhti hai (jpg ya png)
 4. 18 Content Fragments create karti hai
 5. Chaho to publish bhi karti hai

Run:  python create_fragments.py
"""
import sys
import requests

# ===================== CONFIG (sirf yahi badalna) =====================
HOST = "http://localhost:4502"
AUTH = ("admin", "admin")                      # apna username / password

MODEL_PATH = "/conf/multisite/settings/dam/cfm/models/press-release"
DAM_FOLDER = "/content/dam/multisite/press-releases"
IMG_FOLDER = "/content/dam/multisite/press-releases-images"
LINK = "/content/multisite/language-masters/en/press-releases.html"

# Model me publishDate ka type:
#   "date"      -> Date            (value: 2026-10-01)
#   "datetime"  -> Date and Time   (value: 2026-10-01T00:00:00.000Z)
DATE_TYPE = "datetime"

PUBLISH = True        # True = create ke baad publish bhi karo
# =====================================================================

EXPECTED_FIELDS = {"title", "shortDescription", "image",
                   "category", "publishDate", "link"}

A, C, S, W = ("Airports & Aero Services", "Corporate",
              "Sustainability", "Awards & Recognition")

# (name, title, category, date, image number, shortDescription)
DATA = [
 ("fake-job-offers-warning", "Alluri Sitarama Raju International Airport Warns Job Seekers Against Fake Recruitment Offers", A, "2026-10-01", 1, "The airport has cautioned job seekers against fraudulent recruitment offers circulating online."),
 ("delhi-160-destinations", "Delhi Airport strengthens position as India's global aviation hub with 160 destinations", A, "2026-09-23", 2, "Delhi Airport now connects passengers to 160 destinations across the world."),
 ("solar-power-expansion", "Airport expands solar power capacity to 50 MW", S, "2026-09-10", 3, "The expansion marks a major step towards cleaner and renewable airport operations."),
 ("annual-results-fy26", "Annual results announced for FY 2025-26", C, "2026-07-18", 4, "The company reports strong growth in passenger traffic and revenue for the year."),
 ("best-airport-award-2026", "Airport wins Best Airport in Asia-Pacific award", W, "2026-06-05", 5, "The airport has been recognised for outstanding service and passenger experience."),
 ("new-terminal-opening", "New terminal opens for domestic passengers", A, "2026-03-12", 1, "The new terminal increases capacity and improves comfort for domestic travellers."),
 ("digi-yatra-expansion", "Digi Yatra expanded to all boarding gates", A, "2025-12-15", 2, "Passengers can now enjoy faster, paperless boarding at every gate."),
 ("zero-waste-initiative", "Zero waste to landfill initiative launched", S, "2025-10-09", 3, "A new programme aims to recycle and reuse all waste generated at the airport."),
 ("new-board-members", "Board announces appointment of new directors", C, "2025-08-22", 4, "Experienced leaders have joined the board to guide the company's next phase."),
 ("customer-service-award", "Airport honoured for excellence in customer service", W, "2025-08-04", 5, "The award recognises consistent efforts to improve passenger satisfaction."),
 ("cargo-hub-launch", "Dedicated cargo hub launched to boost trade", A, "2025-02-14", 1, "The new cargo hub will speed up freight handling and support regional trade."),
 ("green-airport-certification", "Airport receives Level 5 green certification", S, "2024-11-27", 3, "The certification reflects the airport's commitment to reducing carbon emissions."),
 ("partnership-announcement", "Strategic partnership announced with global airline alliance", C, "2024-09-16", 2, "The partnership will expand international connectivity for passengers."),
 ("passenger-record-2024", "Airport records highest ever monthly passenger traffic", A, "2024-05-30", 4, "Monthly footfall reached an all-time high, driven by strong travel demand."),
 ("safety-excellence-award", "Safety excellence award received for second year", W, "2024-01-19", 5, "The airport continues to set benchmarks in operational safety standards."),
 ("rainwater-harvesting", "Rainwater harvesting project completed", S, "2023-12-08", 3, "The project will help conserve water and reduce dependence on external sources."),
 ("quarterly-results-q2", "Quarterly financial results for Q2 FY 2023-24", C, "2023-10-25", 4, "The company posts steady financial performance for the second quarter."),
 ("runway-upgrade", "Runway upgrade completed ahead of schedule", A, "2023-04-03", 1, "The upgraded runway improves safety and supports higher flight movements."),
]

session = requests.Session()
session.auth = AUTH


def exists(path):
    return session.get(f"{HOST}{path}.json").status_code == 200


def verify_model():
    """Model ke field 'name' values padh ke expected se match karta hai."""
    url = f"{HOST}{MODEL_PATH}/jcr:content/model/cq:dialog/content/items.2.json"
    r = session.get(url)
    if r.status_code != 200:
        print(f"[ERROR] Model nahi mila ({r.status_code}): {MODEL_PATH}")
        sys.exit(1)
    found = {v["name"] for v in r.json().values()
             if isinstance(v, dict) and "name" in v}
    print("Model fields:", sorted(found))
    missing = EXPECTED_FIELDS - found
    if missing:
        print(f"[ERROR] Model me ye fields nahi mile: {sorted(missing)}")
        print("Model ke Property Names check karo ya script ke 'elements' badlo.")
        sys.exit(1)
    print("[OK] Model fields sahi hain\n")


def ensure_folder(dam_path):
    """/content/dam/a/b/c ke har level ko banata hai (agar nahi hai)."""
    parts = dam_path.replace("/content/dam/", "").split("/")
    current = "/content/dam"
    for p in parts:
        current += "/" + p
        if exists(current):
            continue
        api = f"{HOST}/api/assets{current[len('/content/dam'):]}"
        r = session.post(api, json={"class": "assetFolder",
                                    "properties": {"title": p}})
        print(f"  folder {current} -> {r.status_code}")
        if r.status_code not in (200, 201):
            print("[ERROR] Folder nahi ban paya. Assets me manually bana lo.")
            sys.exit(1)


def find_image(n):
    for ext in ("jpg", "jpeg", "png", "webp"):
        p = f"{IMG_FOLDER}/img-{n}.{ext}"
        if exists(p):
            return p
    return None


def date_value(d):
    return f"{d}T00:00:00.000Z" if DATE_TYPE == "datetime" else d


def main():
    verify_model()
    ensure_folder(DAM_FOLDER)

    images = {}
    for n in range(1, 6):
        images[n] = find_image(n)
        if not images[n]:
            print(f"[WARN] img-{n} nahi mili in {IMG_FOLDER} (image khali rahegi)")

    api_base = f"{HOST}/api/assets{DAM_FOLDER[len('/content/dam'):]}"
    ok = skipped = failed = 0

    for name, title, cat, date, img_no, desc in DATA:
        elements = {
            "title":            {"value": title},
            "shortDescription": {"value": desc},
            "category":         {"value": cat},
            "publishDate":      {"value": date_value(date)},
            "link":             {"value": LINK},
        }
        if images[img_no]:
            elements["image"] = {"value": images[img_no]}

        body = {"properties": {
            "cq:model": MODEL_PATH,
            "title": title,
            "description": "",
            "elements": elements,
        }}

        r = session.post(f"{api_base}/{name}", json=body)

        if r.status_code in (200, 201):
            ok += 1
            msg = "created"
        elif r.status_code == 409:
            skipped += 1
            print(f"{name:32} -> already exists, skipped")
            continue
        else:
            failed += 1
            print(f"{name:32} -> FAILED {r.status_code}  {r.text[:150]}")
            continue

        line = f"{name:32} -> {msg}"
        if PUBLISH:
            p = session.post(f"{HOST}/bin/replicate.json",
                             data={"cmd": "activate",
                                   "path": f"{DAM_FOLDER}/{name}"})
            line += f" | publish {p.status_code}"
        print(line)

    print(f"\nDone. Created: {ok}, Skipped: {skipped}, Failed: {failed}")
    if PUBLISH:
        print("Images folder bhi publish karna mat bhoolna "
              "(Assets -> press-releases-images -> Quick Publish).")


if __name__ == "__main__":
    main()