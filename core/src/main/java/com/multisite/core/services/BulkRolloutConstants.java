package com.multisite.core.services;

/**
 * Shared constants for Bulk Rollout feature.
 * TODO: Apne actual site roots yahan customize karo.
 */
public final class BulkRolloutConstants {

    private BulkRolloutConstants() {
        // no instantiation
    }

    // Sirf inhi roots ke neeche bulk rollout allow hoga
    public static final String[] ALLOWED_SITE_ROOTS = {
        "/content/multisite"   // <-- yahan apne actual site root(s) daalo, e.g. "/content/toryburch"
    };

    public static final String PARAM_PATHS = "paths";
    public static final String PARAM_LOCALES = "locales";
    public static final String PARAM_DEEP = "deep";
}