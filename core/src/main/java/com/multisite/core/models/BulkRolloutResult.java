package com.multisite.core.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the outcome of a bulk rollout / eligibility-check operation.
 */
public class BulkRolloutResult {

    // path -> list of available locales (used for eligibility/GET response)
    private final Map<String, List<String>> availableLocales = new HashMap<>();

    // paths that have no eligible live relationship
    private final List<String> skippedPaths = new ArrayList<>();

    // paths successfully rolled out
    private final List<String> rolledOutPaths = new ArrayList<>();

    // path -> error message (for failures during actual rollout)
    private final Map<String, String> failures = new HashMap<>();

    public Map<String, List<String>> getAvailableLocales() {
        return availableLocales;
    }

    public List<String> getSkippedPaths() {
        return skippedPaths;
    }

    public List<String> getRolledOutPaths() {
        return rolledOutPaths;
    }

    public Map<String, String> getFailures() {
        return failures;
    }

    public void addAvailableLocale(String path, String locale) {
    List<String> list = availableLocales.computeIfAbsent(path, k -> new ArrayList<>());
    if (!list.contains(locale)) {
        list.add(locale);
    }
}

    public void addSkipped(String path) {
        skippedPaths.add(path);
    }

    public void addRolledOut(String path) {
        rolledOutPaths.add(path);
    }

    public void addFailure(String path, String message) {
        failures.put(path, message);
    }
}