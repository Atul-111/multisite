package com.multisite.core.models;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

@Model(
        adaptables = org.apache.sling.api.SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ExternalizerModel {

    @OSGiService
    private Externalizer externalizer;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    /**
     * Returns current AEM page path.
     */
    public String getPagePath() {

        if (currentPage == null) {
            return "";
        }

        return currentPage.getPath();
    }

    /**
     * Generates Author URL.
     */
    public String getAuthorUrl() {

        if (currentPage == null
                || externalizer == null
                || resourceResolver == null) {
            return "";
        }

        return externalizer.authorLink(
                resourceResolver,
                currentPage.getPath()
        ) + ".html";
    }

    /**
     * Generates Publish URL.
     */
    public String getPublishUrl() {

        if (currentPage == null
                || externalizer == null
                || resourceResolver == null) {
            return "";
        }

        return externalizer.publishLink(
                resourceResolver,
                currentPage.getPath()
        ) + ".html";
    }

    /**
     * Generates URL using custom
     * "multisite" Externalizer domain.
     */
    public String getMultisiteUrl() {

        if (currentPage == null
                || externalizer == null
                || resourceResolver == null) {
            return "";
        }

        return externalizer.externalLink(
                resourceResolver,
                "multisite",
                currentPage.getPath()
        ) + ".html";
    }
}