package com.multisite.core.services;

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

    // yeh properties translate hongi (har node pe check hongi)
    public static final String[] TRANSLATABLE_PROPERTIES = {
        "jcr:title", "jcr:description", "text", "title",
        "pretitle", "subtitle", "buttonText", "altText"
    };

    public static final String PARAM_PATHS = "paths";
    public static final String PARAM_TARGET_LANGS = "targetLangs";
    public static final String PARAM_SOURCE_LANG = "sourceLang";

    public static String getSourceLanguage(String path) {
        int index = getLocalePathSegmentIndex(path);
        return index < 0 ? null : path.split("/")[index];
    }

    public static int getLocalePathSegmentIndex(String path) {
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
        for (String[] supportedLanguage : SUPPORTED_LANGUAGES) {
            if (supportedLanguage[0].equals(language)) {
                return true;
            }
        }
        return false;
    }
}