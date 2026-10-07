package com.multisite.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TranslationConstantsTest {

    @Test
    void detectsLocaleAfterMarketSegment() {
        assertEquals("en", TranslationConstants.getSourceLanguage("/content/multisite/us/en/page_creation"));
    }

    @Test
    void detectsLocaleUnderLanguageMasters() {
        assertEquals("fr", TranslationConstants.getSourceLanguage("/content/multisite/language-masters/fr/page_creation"));
    }

    @Test
    void doesNotTreatPageNameAsLocale() {
        assertEquals("en", TranslationConstants.getSourceLanguage("/content/multisite/au/en/de"));
    }

    @Test
    void detectsLocaleAfterMarketThatLooksLikeLanguage() {
        assertEquals("en", TranslationConstants.getSourceLanguage("/content/multisite/de/en/page_creation"));
        assertEquals("de", TranslationConstants.getSourceLanguage("/content/multisite/de/de/page_creation"));
    }

    @Test
    void detectsLocaleDirectlyUnderSiteRoot() {
        assertEquals("de", TranslationConstants.getSourceLanguage("/content/multisite/de/page_creation"));
    }

    @Test
    void ignoresPathsWithoutSupportedLocale() {
        assertNull(TranslationConstants.getSourceLanguage("/content/multisite"));
    }
}