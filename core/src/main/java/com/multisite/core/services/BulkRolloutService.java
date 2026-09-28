package com.multisite.core.services;

import java.util.List;
import org.apache.sling.api.resource.ResourceResolver;
import com.multisite.core.models.BulkRolloutResult;

public interface BulkRolloutService {

    /**
     * GET flow: given selected paths, find which locales/live-copies are
     * available and which paths have no eligible live relationship.
     */
    BulkRolloutResult checkEligibility(ResourceResolver resolver, List<String> paths);

    /**
     * POST flow: actually perform the rollout (sync to live copies only,
     * does NOT publish).
     */
    BulkRolloutResult rollout(ResourceResolver resolver, List<String> paths,
                               List<String> locales, boolean deep);
}