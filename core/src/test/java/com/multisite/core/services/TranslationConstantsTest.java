package com.multisite.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void normalizesUppercaseLocaleCodes() {
        assertEquals("it", TranslationConstants.getSourceLanguage("/content/multisite/IT/english/home"));
        assertEquals("it", TranslationConstants.normalizeLanguage("IT"));
        assertTrue(TranslationConstants.isSupportedLanguage("IT"));
    }

    @Test
    void mapsTemplateFooterToTheTargetPageLocale() {
        assertEquals("/content/experience-fragments/multisite/au/en/site/footer/master",
                TranslationConstants.getLocalizedExperienceFragmentPath(
                        "/content/multisite/au/en/en/page_creation",
                        "/content/experience-fragments/multisite/language-masters/en/site/footer/master"));
        assertEquals("en", TranslationConstants.getExperienceFragmentSourceLanguage(
                "/content/multisite/au/en/en/page_creation",
                "/content/experience-fragments/multisite/language-masters/en/site/footer/master"));
    }

    @Test
    void mapsLocalizedExperienceFragmentForItalianEnglishSitePath() {
        assertEquals("/content/experience-fragments/multisite/es/site/footer/master",
                TranslationConstants.getLocalizedExperienceFragmentPath(
                        "/content/multisite/es/english/home",
                        "/content/experience-fragments/multisite/language-masters/en/site/footer/master"));
    }

        @Test
        void prefersLocaleAfterMarketWhenMarketNameIsAlsoSupportedLanguage() {
        assertEquals("de", TranslationConstants.getSourceLanguage("/content/multisite/au/de/en/about"));
        assertEquals("/content/experience-fragments/multisite/au/es/site/footer/master",
            TranslationConstants.getLocalizedExperienceFragmentPath(
                "/content/multisite/au/es/en/about",
                "/content/experience-fragments/multisite/language-masters/en/site/footer/master"));
        }
}