package com.multisite.core.services.impl;

import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.multisite.core.models.TranslationResult;
import com.multisite.core.services.PageTranslationService;
import com.multisite.core.services.TranslationConstants;
import com.multisite.core.services.TranslationProvider;

@Component(service = PageTranslationService.class)
public class PageTranslationServiceImpl implements PageTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(PageTranslationServiceImpl.class);

    @Reference
    private TranslationProvider translationProvider;

    @Override
    public TranslationResult translate(ResourceResolver resolver, List<String> sourcePaths,
                                        List<String> targetLangs, String sourceLang) {
        TranslationResult result = new TranslationResult();
        Session session = resolver.adaptTo(Session.class);

        for (String sourcePath : sourcePaths) {
            if (!isUnderAllowedRoot(sourcePath) || session == null) {
                for (String lang : targetLangs) {
                    result.addSkipped(sourcePath, lang);
                }
                continue;
            }

            for (String targetLang : targetLangs) {
                try {
                    String targetPath = computeTargetPath(sourcePath, sourceLang, targetLang);
                    if (targetPath == null) {
                        result.addSkipped(sourcePath, targetLang);
                        continue;
                    }

                    ensureCopyExists(session, sourcePath, targetPath);
                    Node targetNode = session.getNode(targetPath);
                    int propertiesTranslated = translateNodeRecursive(targetNode, sourceLang, targetLang);

                    session.save();
                    result.addTranslated(sourcePath, targetLang, targetPath);
                    for (int i = 0; i < propertiesTranslated; i++) {
                        result.incrementPropertyCount();
                    }
                } catch (Exception e) {
                    LOG.error("Translation failed for {} -> {}", sourcePath, targetLang, e);
                    try {
                        session.refresh(false);
                    } catch (Exception ignore) { /* no-op */ }
                    result.addFailure(sourcePath, targetLang, e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        }
        return result;
    }

    /** Agar target page exist nahi karta, poora subtree copy karta hai. */
    private void ensureCopyExists(Session session, String sourcePath, String targetPath) throws Exception {
        if (session.itemExists(targetPath)) {
            return;
        }
        String targetParent = targetPath.substring(0, targetPath.lastIndexOf('/'));
        ensureParentExists(session, targetParent);
        session.getWorkspace().copy(sourcePath, targetPath);
    }

    private void ensureParentExists(Session session, String path) throws Exception {
        if (session.itemExists(path) || "/".equals(path)) {
            return;
        }
        String parent = path.substring(0, path.lastIndexOf('/'));
        ensureParentExists(session, parent.isEmpty() ? "/" : parent);
        String name = path.substring(path.lastIndexOf('/') + 1);
        session.getNode(parent.isEmpty() ? "/" : parent).addNode(name, "sling:Folder");
    }

    /** jcr:content tree ke andar recursively translatable properties translate karta hai. */
    private int translateNodeRecursive(Node node, String sourceLang, String targetLang) throws Exception {
        int propertiesTranslated = 0;
        PropertyIterator props = node.getProperties();
        while (props.hasNext()) {
            Property prop = props.nextProperty();
            String name = prop.getName();
            if (!isTranslatable(name) || prop.isMultiple()) {
                continue;
            }
            if (prop.getType() != javax.jcr.PropertyType.STRING) {
                continue;
            }
            String value = prop.getString();
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                node.setProperty(name, translationProvider.translate(value, sourceLang, targetLang));
                propertiesTranslated++;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to translate property '" + name
                        + "' at " + node.getPath() + ": " + e.getMessage(), e);
            }
        }

        NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (!child.getName().startsWith("jcr:") || "jcr:content".equals(child.getName())) {
                propertiesTranslated += translateNodeRecursive(child, sourceLang, targetLang);
            }
        }
        return propertiesTranslated;
    }

    private boolean isTranslatable(String propName) {
        for (String p : TranslationConstants.TRANSLATABLE_PROPERTIES) {
            if (p.equals(propName)) {
                return true;
            }
        }
        return false;
    }

    /** /content/multisite/en/home -> /content/multisite/fr/home */
    private String computeTargetPath(String sourcePath, String sourceLang, String targetLang) {
        String[] segments = sourcePath.split("/");
        int idx = TranslationConstants.getLocalePathSegmentIndex(sourcePath);
        if (idx < 0 || sourceLang == null || !sourceLang.equals(segments[idx])
                || !TranslationConstants.isSupportedLanguage(targetLang) || targetLang.equals(sourceLang)) {
            return null;
        }
        segments[idx] = targetLang;
        return String.join("/", segments);
    }

    private boolean isUnderAllowedRoot(String path) {
        for (String root : TranslationConstants.ALLOWED_SITE_ROOTS) {
            if (path.equals(root) || path.startsWith(root + "/")) {
                return true;
            }
        }
        return false;
    }
}