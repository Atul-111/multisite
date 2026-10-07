package com.multisite.core.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TranslationResult {

    // "sourcePath|lang" -> targetPath
    private final Map<String, String> translated = new LinkedHashMap<>();
    private final List<String> skipped = new ArrayList<>();
    // "sourcePath|lang" -> error message
    private final Map<String, String> failures = new LinkedHashMap<>();
    private int propertiesTranslatedCount = 0;

    public Map<String, String> getTranslated() { return translated; }
    public List<String> getSkipped() { return skipped; }
    public Map<String, String> getFailures() { return failures; }
    public int getPropertiesTranslatedCount() { return propertiesTranslatedCount; }

    public void addTranslated(String sourcePath, String lang, String targetPath) {
        translated.put(sourcePath + "|" + lang, targetPath);
    }
    public void addSkipped(String sourcePath, String lang) {
        skipped.add(sourcePath + "|" + lang);
    }
    public void addFailure(String sourcePath, String lang, String message) {
        failures.put(sourcePath + "|" + lang, message);
    }
    public void incrementPropertyCount() {
        propertiesTranslatedCount++;
    }
}