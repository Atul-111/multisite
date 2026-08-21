package com.multisite.core.servlets;

import com.multisite.core.services.AIChatService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/multisite/chat")
public class AIChatServlet extends SlingAllMethodsServlet {

    private static final Logger LOG =
            LoggerFactory.getLogger(AIChatServlet.class);

    @Reference
    private AIChatService aiChatService;

    protected void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message =
                request.getParameter("message");

        if (message == null || message.trim().isEmpty()) {

            response.setStatus(
                    SlingHttpServletResponse.SC_BAD_REQUEST
            );

                        try {
                                JSONObject error = new JSONObject();
                                error.put("error", "Message is required.");
                                response.getWriter().write(error.toString());
                        } catch (org.json.JSONException je) {
                                LOG.error("JSON error creating error response", je);
                                response.getWriter().write("{\"error\":\"Message is required.\"}");
                        } catch (Exception e) {
                                LOG.error("Error creating error response", e);
                        }

            return;
        }

        try {

            LOG.debug(
                    "AI chatbot request received."
            );

            String answer =
                    aiChatService.ask(message);

            JSONObject result =
                    new JSONObject();

            result.put(
                    "answer",
                    answer
            );

            response.setStatus(
                    SlingHttpServletResponse.SC_OK
            );

            response.getWriter().write(
                    result.toString()
            );

        } catch (Exception e) {

            LOG.error(
                    "Error while processing AI chatbot request.",
                    e
            );

            response.setStatus(
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            try {
                JSONObject error = new JSONObject();
                error.put("error", "Unable to process AI request.");
                response.getWriter().write(error.toString());
            } catch (org.json.JSONException je) {
                LOG.error("JSON error creating error response", je);
                response.getWriter().write("{\"error\":\"Unable to process AI request.\"}");
            }
        }
    }
}