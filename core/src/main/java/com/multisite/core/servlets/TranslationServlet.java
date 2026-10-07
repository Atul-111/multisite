package com.multisite.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multisite.core.models.TranslationResult;
import com.multisite.core.services.PageTranslationService;
import com.multisite.core.services.TranslationConstants;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/multisite/translate",
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
    })
public class TranslationServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TranslationServlet.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private transient PageTranslationService pageTranslationService;

    // GET -> supported languages list (console page ke liye)
    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(200);
        if ("pages".equals(req.getParameter("mode"))) {
            MAPPER.writeValue(resp.getWriter(), getPageOptions(req));
            return;
        }
        MAPPER.writeValue(resp.getWriter(), TranslationConstants.SUPPORTED_LANGUAGES);
    }

    private Map<String, Object> getPageOptions(SlingHttpServletRequest req) {
        List<Map<String, String>> pages = new ArrayList<>();
        PageManager pageManager = req.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager != null) {
            for (String rootPath : TranslationConstants.ALLOWED_SITE_ROOTS) {
                Resource root = req.getResourceResolver().getResource(rootPath);
                if (root != null) {
                    collectPages(root, pageManager, pages);
                }
            }
        }
        pages.sort(Comparator.comparing(page -> page.get("path")));

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("languages", TranslationConstants.SUPPORTED_LANGUAGES);
        options.put("pages", pages);
        return options;
    }

    private void collectPages(Resource resource, PageManager pageManager, List<Map<String, String>> pages) {
        Page page = pageManager.getPage(resource.getPath());
        if (page != null) {
            collectPage(page, pages);
            return;
        }
        for (Resource child : resource.getChildren()) {
            if (!"jcr:content".equals(child.getName())) {
                collectPages(child, pageManager, pages);
            }
        }
    }

    private void collectPage(Page page, List<Map<String, String>> pages) {
        String sourceLanguage = TranslationConstants.getSourceLanguage(page.getPath());
        if (sourceLanguage != null) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("title", page.getTitle() == null ? page.getName() : page.getTitle());
            item.put("path", page.getPath());
            item.put("sourceLanguage", sourceLanguage);
            pages.add(item);
        }

        Iterator<Page> children = page.listChildren();
        while (children.hasNext()) {
            collectPage(children.next(), pages);
        }
    }

    // POST -> actual translation trigger
    @Override
    protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws IOException {
        TranslationResult result = new TranslationResult();
        try {
            List<String> paths = list(req, TranslationConstants.PARAM_PATHS);
            List<String> targetLangs = list(req, TranslationConstants.PARAM_TARGET_LANGS);
            String sourceLang = req.getParameter(TranslationConstants.PARAM_SOURCE_LANG);

            result = pageTranslationService.translate(req.getResourceResolver(), paths, targetLangs, sourceLang);
        } catch (Exception e) {
            LOG.error("Translation request failed", e);
            result.addFailure("*", "*", String.valueOf(e.getMessage()));
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(200);
        MAPPER.writeValue(resp.getWriter(), result);
    }

    private List<String> list(SlingHttpServletRequest req, String name) {
        String[] v = req.getParameterValues(name);
        return v == null ? Collections.<String>emptyList() : Arrays.asList(v);
    }
}