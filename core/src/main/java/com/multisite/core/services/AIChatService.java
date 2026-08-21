package com.multisite.core.services;

/**
 * Service interface for communicating with an AI provider.
 */
public interface AIChatService {

    /**
     * Sends a user message to the configured AI provider
     * and returns the generated response.
     *
     * @param message user message
     * @return AI generated response
     */
    String ask(String message);
}