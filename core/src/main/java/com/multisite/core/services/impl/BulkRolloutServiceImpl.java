package com.multisite.core.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.msm.api.LiveStatus;
import com.day.cq.wcm.msm.api.RolloutManager;
import com.multisite.core.models.BulkRolloutResult;
import com.multisite.core.services.BulkRolloutConstants;
import com.multisite.core.services.BulkRolloutService;

@Component(service = BulkRolloutService.class)
public class BulkRolloutServiceImpl implements BulkRolloutService {

    private static final Logger LOG = LoggerFactory.getLogger(BulkRolloutServiceImpl.class);

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Reference
    private RolloutManager rolloutManager;

    /**
     * GET flow: har selected path ke liye available locales nikalta hai.
     * Jis path pe koi active live copy nahi, use skipped mein daalta hai.
     */
    @Override
    public BulkRolloutResult checkEligibility(ResourceResolver resolver, List<String> paths) {
        BulkRolloutResult result = new BulkRolloutResult();

        for (String path : paths) {
            if (!isUnderAllowedRoot(path)) {
                result.addSkipped(path);
                continue;
            }

            boolean eligible = false;
            for (Page page : collectPages(resolver, path)) {
                for (LiveRelationship rel : getActiveRelationships(page)) {
                    result.addAvailableLocale(path, toLocaleLabel(rel));
                    eligible = true;
                }
            }

            if (!eligible) {
                result.addSkipped(path);
            }
        }
        return result;
    }

    /**
     * POST flow: sirf selected locales pe rollout karta hai.
     * Trigger.ROLLOUT use hota hai, isliye publish NAHI hota.
     * locales empty ho toh saare eligible locales pe rollout hota hai.
     */
    @Override
    public BulkRolloutResult rollout(ResourceResolver resolver, List<String> paths,
                                     List<String> locales, boolean deep) {
        BulkRolloutResult result = new BulkRolloutResult();

        for (String path : paths) {
            if (!isUnderAllowedRoot(path)) {
                result.addSkipped(path);
                continue;
            }

            boolean anyRolledOut = false;
            try {
                for (Page page : collectPages(resolver, path)) {

                    List<String> targets = new ArrayList<>();
                    for (LiveRelationship rel : getActiveRelationships(page)) {
                        String label = toLocaleLabel(rel);
                        if (locales.isEmpty() || locales.contains(label)) {
                            targets.add(rel.getTargetPath());
                        }
                    }
                    if (targets.isEmpty()) {
                        continue;
                    }

                    RolloutManager.RolloutParams params = new RolloutManager.RolloutParams();
                    params.master = page;
                    params.isDeep = deep;
                    params.reset = false;
                    params.targets = targets.toArray(new String[0]);
                    params.trigger = RolloutManager.Trigger.ROLLOUT; // rollout only, no publish
                    rolloutManager.rollout(params);
                    anyRolledOut = true;
                }

                if (resolver.hasChanges()) {
                    resolver.commit();
                }

                if (anyRolledOut) {
                    result.addRolledOut(path);
                } else {
                    result.addSkipped(path);
                }
            } catch (Exception e) {
                LOG.error("Bulk rollout failed for {}", path, e);
                try {
                    resolver.refresh();
                } catch (Exception refreshEx) {
                    LOG.warn("Could not refresh resolver after failure", refreshEx);
                }
                result.addFailure(path, e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }
        return result;
    }

/**
 * Page ki SAARI live copies (koi bhi rollout config / trigger ho).
 * Sirf woh chhodta hai jinki inheritance cancel hai, kyunki un par rollout kuch nahi karta.
 */
private List<LiveRelationship> getActiveRelationships(Page page) {
    List<LiveRelationship> active = new ArrayList<>();
    try {
        Collection<LiveRelationship> rels =
                liveRelationshipManager.getLiveRelationships(page, null, null, false);
        if (rels != null) {
            for (LiveRelationship rel : rels) {
                LiveStatus status = rel.getStatus();
                if (status == null || !status.isCancelled()) {
                    active.add(rel);
                }
            }
        }
    } catch (WCMException e) {
        LOG.error("Could not read live relationships for {}", page.getPath(), e);
    }
    return active;
}

    /**
     * Path agar Page hai toh wahi; folder hai toh uske andar ke saare pages.
     */
    private List<Page> collectPages(ResourceResolver resolver, String path) {
        List<Page> pages = new ArrayList<>();
        Resource res = resolver.getResource(path);
        if (res != null) {
            collect(res, pages);
        }
        return pages;
    }

    private void collect(Resource res, List<Page> pages) {
        Page page = res.adaptTo(Page.class);
        if (page != null) {
            // deep flag ke saath page ke children ko RolloutManager khud handle karta hai
            pages.add(page);
            return;
        }
        for (Resource child : res.getChildren()) {
            if (!"jcr:content".equals(child.getName())) {
                collect(child, pages);
            }
        }
    }

    /**
     * Target path ko site root ke relative kaat kar locale label banata hai.
     * /content/multisite/us/en/home -> US-EN
     */
    private String toLocaleLabel(LiveRelationship rel) {
        String target = rel.getTargetPath();
        for (String root : BulkRolloutConstants.ALLOWED_SITE_ROOTS) {
            if (target.startsWith(root + "/")) {
                String[] seg = target.substring(root.length() + 1).split("/");
                if (seg.length >= 2) {
                    return (seg[0] + "-" + seg[1]).toUpperCase();
                }
                return seg[0].toUpperCase();
            }
        }
        return target;
    }

    private boolean isUnderAllowedRoot(String path) {
        for (String root : BulkRolloutConstants.ALLOWED_SITE_ROOTS) {
            if (path.equals(root) || path.startsWith(root + "/")) {
                return true;
            }
        }
        return false;
    }
}