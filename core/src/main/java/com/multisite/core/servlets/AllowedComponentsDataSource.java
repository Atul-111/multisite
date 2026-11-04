package com.multisite.core.servlets;

import java.util.*;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.wcm.api.components.ComponentManager;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;

@Component(service = Servlet.class, property = {
    "sling.servlet.resourceTypes=multisite/datasources/allowed-components",
    "sling.servlet.methods=GET"
})
public class AllowedComponentsDataSource extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> options = new ArrayList<>();

        // Dialog open hone par suffix = selector component ka content path
        String suffix = request.getRequestPathInfo().getSuffix();
        if (suffix != null) {
            // Dialog URL mein _jcr_content aata hai, JCR mein asli naam jcr:content hai
            suffix = suffix.replace("/_jcr_content", "/jcr:content");
        }
        Resource selector = suffix != null ? resolver.getResource(suffix) : null;
        Resource container = selector != null ? selector.getParent() : null;

        if (container != null) {
            ContentPolicyManager cpm = resolver.adaptTo(ContentPolicyManager.class);
            ContentPolicy policy = cpm != null ? cpm.getPolicy(container) : null;
            String[] allowed = policy != null
                ? policy.getProperties().get("components", new String[0]) : new String[0];

            ComponentManager compMgr = resolver.adaptTo(ComponentManager.class);
            Set<String> seen = new LinkedHashSet<>();

            for (String entry : allowed) {
                if (entry.startsWith("group:")) {
                    String group = entry.substring(6);
                    if (compMgr != null) {
                        compMgr.getComponents().stream()
                            .filter(c -> group.equals(c.getComponentGroup()))
                            .forEach(c -> addOption(options, resolver, seen, c.getPath(), c.getTitle()));
                    }
                } else {
                    Resource r = resolver.getResource(entry);
                    String title = r != null ? r.getValueMap().get("jcr:title", r.getName()) : entry;
                    addOption(options, resolver, seen, entry, title);
                }
            }
        }
        request.setAttribute(DataSource.class.getName(), new SimpleDataSource(options.iterator()));
    }

    private void addOption(List<Resource> options, ResourceResolver resolver,
                           Set<String> seen, String path, String title) {
        // /apps/multisite/components/button -> multisite/components/button
        String resourceType = path.replaceFirst("^/(apps|libs)/", "");
        if (!seen.add(resourceType) || resourceType.endsWith("/componentselector")) {
            return;
        }
        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
        vm.put("value", resourceType);
        vm.put("text", title);
        options.add(new ValueMapResource(resolver, new ResourceMetadata(), "nt:unstructured", vm));
    }
}