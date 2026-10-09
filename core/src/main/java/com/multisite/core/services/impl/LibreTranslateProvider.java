package com.multisite.core.services.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.multisite.core.services.TranslationProvider;

@Component(service = TranslationProvider.class, immediate = true)
@Designate(ocd = LibreTranslateProvider.Config.class)
public class LibreTranslateProvider implements TranslationProvider {

    @ObjectClassDefinition(name = "Multisite - LibreTranslate Provider")
    public @interface Config {
        @AttributeDefinition(name = "LibreTranslate translate endpoint")
        String endpoint() default "http://127.0.0.1:5000/translate";

        @AttributeDefinition(name = "API key (optional for local LibreTranslate)", type = org.osgi.service.metatype.annotations.AttributeType.PASSWORD)
        String apiKey() default "";
    }

    private static final Logger LOG = LoggerFactory.getLogger(LibreTranslateProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private volatile String endpoint;
    private volatile String apiKey;

    @Activate
    @Modified
    protected void activate(Config config) {
        endpoint = config.endpoint();
        apiKey = config.apiKey();
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        if (text == null || text.isBlank()) {
            return text;
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("LibreTranslate endpoint is not configured.");
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("q", text);
        payload.put("source", sourceLang == null || sourceLang.isBlank() ? "auto" : sourceLang);
        payload.put("target", targetLang);
        payload.put("format", "html");
        if (apiKey != null && !apiKey.isBlank()) {
            payload.put("api_key", apiKey);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response;
        try {
            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not reach LibreTranslate. Verify the configured endpoint is reachable from AEM "
                            + "and that the LibreTranslate service is running.",
                    e);
        }
        if (response.statusCode() != 200) {
            String message = getErrorMessage(response.body());
            LOG.error("LibreTranslate API error {}: {}", response.statusCode(), message);
            throw new RuntimeException("LibreTranslate returned HTTP " + response.statusCode() + ": " + message);
        }

        JsonNode translatedText = MAPPER.readTree(response.body()).path("translatedText");
        if (translatedText.isMissingNode() || translatedText.isNull()) {
            throw new IllegalStateException("LibreTranslate response did not contain translatedText.");
        }
        return translatedText.asText();
    }

    private static String getErrorMessage(String responseBody) {
        try {
            JsonNode response = MAPPER.readTree(responseBody);
            String message = response.path("error").asText(response.path("message").asText());
            return message.isBlank() ? "No error details returned." : message;
        } catch (Exception e) {
            return "No error details returned.";
        }
    }
}