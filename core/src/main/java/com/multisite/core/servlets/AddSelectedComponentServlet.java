package com.multisite.core.servlets;

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class, property = {
        "sling.servlet.paths=/bin/multisite/addcomponent",
        "sling.servlet.methods=POST"
})
public class AddSelectedComponentServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AddSelectedComponentServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        // Logged-in author ka session, isliye ACL respect hoga
        ResourceResolver resolver = request.getResourceResolver();
        String selectorPath = request.getParameter("path");
        String resourceType = request.getParameter("resourceType");

        log.info("addcomponent called: path={}, resourceType={}", selectorPath, resourceType);

        if (resourceType == null || resourceType.isEmpty()) {
            response.sendError(400, "resourceType missing");
            return;
        }
        if (selectorPath == null || selectorPath.isEmpty()) {
            response.sendError(400, "path missing");
            return;
        }

        // Agar path mein extension aa gaya ho toh hata do
        int dot = selectorPath.indexOf('.', selectorPath.lastIndexOf('/'));
        if (dot > -1) {
            selectorPath = selectorPath.substring(0, dot);
        }

        // Dialog form action mein _jcr_content aata hai, JCR mein asli naam jcr:content hai
        selectorPath = selectorPath.replace("/_jcr_content", "/jcr:content");

        Resource selector = resolver.getResource(selectorPath);
        if (selector == null) {
            response.sendError(400, "selector resource not found: " + selectorPath);
            return;
        }
        Resource parent = selector.getParent();
        if (parent == null) {
            response.sendError(400, "selector has no parent: " + selectorPath);
            return;
        }

        try {
            Session session = resolver.adaptTo(Session.class);
            Node parentNode = parent.adaptTo(Node.class);
            Node selectorNode = selector.adaptTo(Node.class);

            // Naya node banne se PEHLE next sibling nikalo
            String nextSibling = nextSiblingName(parent, selector.getName());

            String base = resourceType.substring(resourceType.lastIndexOf('/') + 1);
            String name = base + "_" + System.currentTimeMillis();

            Node newNode = parentNode.addNode(name, "nt:unstructured");
            newNode.setProperty("sling:resourceType", resourceType);

            // Selector ke theek baad position (null = end mein)
            parentNode.orderBefore(name, nextSibling);

            // Selector hata do
            selectorNode.remove();
            session.save();

            log.info("Added {} at {}/{}", resourceType, parent.getPath(), name);
            response.setStatus(200);
        } catch (Exception e) {
            log.error("Add component failed for {}", selectorPath, e);
            response.sendError(500, e.getMessage());
        }
    }

    private String nextSiblingName(Resource parent, String selectorName) {
        Iterator<Resource> it = parent.listChildren();
        while (it.hasNext()) {
            if (it.next().getName().equals(selectorName)) {
                return it.hasNext() ? it.next().getName() : null;
            }
        }
        return null;
    }
}