package com.multisite.core.servlets;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multisite.core.models.BulkRolloutResult;
import com.multisite.core.services.BulkRolloutConstants;
import com.multisite.core.services.BulkRolloutService;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/multisite/bulkrollout",
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
    }
)
public class BulkRolloutServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(BulkRolloutServlet.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private transient BulkRolloutService bulkRolloutService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        // GET -> eligibility check / discover available locales
        String[] pathsParam = request.getParameterValues(BulkRolloutConstants.PARAM_PATHS);
        List<String> paths = pathsParam != null ? Arrays.asList(pathsParam) : List.of();

        ResourceResolver resolver = request.getResourceResolver();
        BulkRolloutResult result = bulkRolloutService.checkEligibility(resolver, paths);

        writeJson(response, result);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        String[] pathsParam = request.getParameterValues(BulkRolloutConstants.PARAM_PATHS);
        String[] localesParam = request.getParameterValues(BulkRolloutConstants.PARAM_LOCALES);
        boolean deep = Boolean.parseBoolean(request.getParameter(BulkRolloutConstants.PARAM_DEEP));

        List<String> paths = pathsParam != null ? Arrays.asList(pathsParam) : List.of();
        List<String> locales = localesParam != null ? Arrays.asList(localesParam) : List.of();

        ResourceResolver resolver = request.getResourceResolver();
        BulkRolloutResult result = bulkRolloutService.rollout(resolver, paths, locales, deep);

        writeJson(response, result);
    }

    // Always return HTTP 200 + JSON, even on logical failure —
    // AEM can swallow non-2xx bodies on some paths.
    private void writeJson(SlingHttpServletResponse response, BulkRolloutResult result)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        try {
            MAPPER.writeValue(response.getWriter(), result);
        } catch (Exception e) {
            LOG.error("Error writing JSON response", e);
        }
    }
}