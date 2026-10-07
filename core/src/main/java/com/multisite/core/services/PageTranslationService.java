package com.multisite.core.services;

import java.util.List;
import org.apache.sling.api.resource.ResourceResolver;
import com.multisite.core.models.TranslationResult;

public interface PageTranslationService {

    /**
     * Har source path ko har target language mein translate karta hai.
     */
    TranslationResult translate(ResourceResolver resolver, List<String> sourcePaths,
                                 List<String> targetLangs, String sourceLang);
}