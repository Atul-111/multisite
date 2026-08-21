package com.multisite.core.services.impl;

import com.multisite.core.services.AIChatService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@Component(service = AIChatService.class)
@Designate(ocd = GeminiChatServiceImpl.Config.class)
public class GeminiChatServiceImpl implements AIChatService {

    private static final Logger LOG =
            LoggerFactory.getLogger(GeminiChatServiceImpl.class);

    private String apiKey;
    private String endpoint;
    private String model;
    private int timeoutSeconds;

    @ObjectClassDefinition(
            name = "Multisite - Gemini AI Chat Service"
    )
    public @interface Config {

        @AttributeDefinition(
                name = "Gemini API Key",
                description = "Gemini API key"
        )
        String apiKey() default "";

        @AttributeDefinition(
                name = "Gemini Endpoint",
                description = "Gemini generateContent endpoint"
        )
        String endpoint() default
                "https://generativelanguage.googleapis.com/v1beta/models";

        @AttributeDefinition(
                name = "Gemini Model",
                description = "Gemini model name"
        )
        String model() default "gemini-3.6-flash";

        @AttributeDefinition(
                name = "Timeout Seconds",
                description = "HTTP timeout in seconds"
        )
        int timeoutSeconds() default 30;
    }

    @Activate
    protected void activate(Config config) {

        this.apiKey = config.apiKey();
        this.endpoint = config.endpoint();
        this.model = config.model();
        this.timeoutSeconds = config.timeoutSeconds();

        LOG.info(
                "Gemini Chat Service activated. Model: {}, Endpoint: {}",
                model,
                endpoint
        );
    }

    @Override
    public String ask(String message) {

        if (message == null || message.trim().isEmpty()) {
            return "Please enter a message.";
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOG.error("Gemini API key is not configured.");
            return "Gemini API key is not configured.";
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            String url = endpoint
                    + "/"
                    + model
                    + ":generateContent";

            LOG.debug("Calling Gemini API: {}", url);

            HttpPost httpPost = new HttpPost(url);

            /*
             * Gemini API authentication.
             *
             * Do NOT log the API key.
             */
            httpPost.setHeader(
                    "x-goog-api-key",
                    apiKey
            );

            httpPost.setHeader(
                    "Content-Type",
                    "application/json"
            );

            /*
             * Gemini request body.
             *
             * Example:
             *
             * {
             *   "contents": [
             *     {
             *       "parts": [
             *         {
             *           "text": "What is AEM?"
             *         }
             *       ]
             *     }
             *   ]
             * }
             */

            JSONObject textPart = new JSONObject();
            textPart.put("text", message);

            JSONArray parts = new JSONArray();
            parts.put(textPart);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            StringEntity entity = new StringEntity(
                    requestBody.toString(),
                    ContentType.APPLICATION_JSON
            );

            httpPost.setEntity(entity);

            try (CloseableHttpResponse response =
                         httpClient.execute(httpPost)) {

                int statusCode =
                        response.getStatusLine().getStatusCode();

                String responseBody =
                        response.getEntity() != null
                                ? EntityUtils.toString(
                                        response.getEntity(),
                                        StandardCharsets.UTF_8)
                                : "";

                LOG.debug(
                        "Gemini response status: {}",
                        statusCode
                );

                if (statusCode >= 200 && statusCode < 300) {

                    return extractGeminiText(responseBody);

                } else {

                    LOG.error(
                            "Gemini API request failed. HTTP status={}",
                            statusCode
                    );

                    LOG.error(
                            "Gemini API response: {}",
                            responseBody
                    );

                    return "Unable to get response from Gemini AI service.";
                }
            }

        } catch (Exception e) {

            LOG.error(
                    "Exception while calling Gemini API.",
                    e
            );

            return "Unable to connect to Gemini AI service.";
        }
    }

    /**
     * Extracts the generated text from Gemini response.
     *
     * Typical response:
     *
     * {
     *   "candidates": [
     *     {
     *       "content": {
     *         "parts": [
     *           {
     *             "text": "..."
     *           }
     *         ]
     *       }
     *     }
     *   ]
     * }
     */
    private String extractGeminiText(String responseBody) {

        try {

            JSONObject responseJson =
                    new JSONObject(responseBody);

            JSONArray candidates =
                    responseJson.optJSONArray("candidates");

            if (candidates == null || candidates.length() == 0) {

                LOG.warn(
                        "Gemini response does not contain candidates."
                );

                return "Gemini returned an empty response.";
            }

            JSONObject firstCandidate =
                    candidates.getJSONObject(0);

            JSONObject content =
                    firstCandidate.optJSONObject("content");

            if (content == null) {
                return "Gemini returned an empty response.";
            }

            JSONArray parts =
                    content.optJSONArray("parts");

            if (parts == null || parts.length() == 0) {
                return "Gemini returned an empty response.";
            }

            JSONObject firstPart =
                    parts.getJSONObject(0);

            String text =
                    firstPart.optString("text", "");

            if (text.isEmpty()) {
                return "Gemini returned an empty response.";
            }

            return text;

        } catch (Exception e) {

            LOG.error(
                    "Unable to parse Gemini response.",
                    e
            );

            return "Unable to process Gemini response.";
        }
    }
}