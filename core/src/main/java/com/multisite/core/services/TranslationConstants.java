package com.multisite.core.services;

import java.util.Arrays;
import java.util.Locale;

public final class TranslationConstants {

    private TranslationConstants() { }

    // TODO: apne actual site roots daalo
    public static final String[] ALLOWED_SITE_ROOTS = { "/content/multisite" };

    // supported target languages: code -> display name
    public static final String[][] SUPPORTED_LANGUAGES = {
        { "en", "English" },
        { "fr", "French" },
        { "de", "German" },
        { "es", "Spanish" },
        { "ja", "Japanese" },
        { "it", "Italian" }
    };

    // Text-only JCR fields; identifiers, paths, and component configuration are intentionally excluded.
    public static final String[] TRANSLATABLE_PROPERTIES = {
        "jcr:title", "jcr:description", "text", "title", "description",
        "pretitle", "subtitle", "buttonText", "altText", "alt",
        "label", "placeholder", "caption", "linkText", "linkLabel", "buttonLabel", "navigationTitle",
        "pageTitle", "accessibilityLabel", "ariaLabel", "tabTitle",
        "cq:panelTitle", "chatbotTitle", "welcomeMessage", "inputLabel",
        "inputPlaceholder", "sendLabel", "errorMessage"
    };

    public static final String PARAM_PATHS = "paths";
    public static final String PARAM_TARGET_LANGS = "targetLangs";
    public static final String PARAM_SOURCE_LANG = "sourceLang";

    public static String normalizeLanguage(String language) {
        if (language == null) {
            return null;
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    public static String getSourceLanguage(String path) {
        int index = getLocalePathSegmentIndex(path);
        if (index < 0) {
            return null;
        }
        return normalizeLanguage(path.split("/")[index]);
    }

    public static int getLocalePathSegmentIndex(String path) {
        if (path == null || path.isBlank()) {
            return -1;
        }
        String[] segments = path.split("/");
        if (segments.length > 4 && isSupportedLanguage(segments[4])) {
            return 4;
        }
        if (segments.length > 3 && isSupportedLanguage(segments[3])) {
            return 3;
        }
        return -1;
    }

    public static boolean isSupportedLanguage(String language) {
        String normalizedLanguage = normalizeLanguage(language);
        if (normalizedLanguage == null || normalizedLanguage.isBlank()) {
            return false;
        }
        for (String[] supportedLanguage : SUPPORTED_LANGUAGES) {
            if (supportedLanguage[0].equals(normalizedLanguage)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTranslatableProperty(String propertyName) {
        for (String translatableProperty : TRANSLATABLE_PROPERTIES) {
            if (translatableProperty.equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    public static String getLocalizedExperienceFragmentPath(String pagePath, String fragmentPath) {
        int pageLocaleIndex = getLocalePathSegmentIndex(pagePath);
        if (pageLocaleIndex < 0 || fragmentPath == null || !fragmentPath.startsWith("/content/experience-fragments/")) {
            return null;
        }

        String[] pageSegments = pagePath.split("/");
        String[] fragmentSegments = fragmentPath.split("/");

        int targetRootLength = pageLocaleIndex + 1;

        int sourceRootLength = 5;
        if (fragmentSegments.length > 5 && "language-masters".equals(fragmentSegments[4])
                && fragmentSegments.length > 6 && isSupportedLanguage(fragmentSegments[5])) {
            sourceRootLength = 6;
        }

        if (fragmentSegments.length <= sourceRootLength) {
            return null;
        }

        String sourceRoot = String.join("/", Arrays.copyOf(fragmentSegments, sourceRootLength));
        String targetRoot = String.join("/", Arrays.copyOf(pageSegments, targetRootLength))
                .replaceFirst("^/content", "/content/experience-fragments");
        return targetRoot + fragmentPath.substring(sourceRoot.length());
    }

    public static String getExperienceFragmentSourceLanguage(String pagePath, String fragmentPath) {
        if (pagePath == null || fragmentPath == null) {
            return null;
        }
        String[] fragmentSegments = fragmentPath.split("/");
        for (int i = 0; i < fragmentSegments.length - 1; i++) {
            if ("language-masters".equals(fragmentSegments[i]) && isSupportedLanguage(fragmentSegments[i + 1])) {
                return normalizeLanguage(fragmentSegments[i + 1]);
            }
        }
        for (String segment : fragmentSegments) {
            if (isSupportedLanguage(segment)) {
                return normalizeLanguage(segment);
            }
        }
        return null;
    }
}