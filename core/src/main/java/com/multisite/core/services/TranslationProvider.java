package com.multisite.core.services;

public interface TranslationProvider {

    /**
     * Ek text string ko target language mein translate karta hai.
     * @param text        original text (plain ya simple HTML)
     * @param sourceLang  source language code, e.g. "en" (null/empty ho toh auto-detect)
     * @param targetLang  target language code, e.g. "fr"
     */
    String translate(String text, String sourceLang, String targetLang) throws Exception;
}