
package com.multisite.core.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.mail.SimpleEmail;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

import com.multisite.core.services.AssetNotificationService;

@Component(service = AssetNotificationService.class)
public class AssetNotificationServiceImpl
        implements AssetNotificationService {

    private static final Logger LOG =
        LoggerFactory.getLogger(AssetNotificationServiceImpl.class);

    private static final String OWNER_EMAIL_PROPERTY =
        "assetOwnerEmail";

    private static final String SENDER_EMAIL =
        "aem-notifications@example.com";

    private static final String SUBSERVICE =
        "multisite-notification";

    @Reference
    private MessageGatewayService messageGatewayService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void sendNotification(String assetPath, String action)
            throws Exception {

        if (assetPath == null || assetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset path is required");
        }

        if (!"ACTIVATE".equals(action)
                && !"DEACTIVATE".equals(action)) {
            throw new IllegalArgumentException(
                "Unsupported action: " + action);
        }

        String ownerEmail = getOwnerEmail(assetPath);

        if (!isPlausibleEmail(ownerEmail)) {
            throw new IllegalStateException(
                "Valid assetOwnerEmail is missing for " + assetPath);
        }

        MessageGateway<SimpleEmail> gateway =
            messageGatewayService.getGateway(SimpleEmail.class);

        if (gateway == null) {
            throw new IllegalStateException(
                "AEM SimpleEmail gateway is unavailable");
        }

        String status = "ACTIVATE".equals(action)
            ? "Published" : "Unpublished";

        SimpleEmail email = new SimpleEmail();
        email.setFrom(SENDER_EMAIL);
        email.addTo(ownerEmail);
        email.setSubject("AEM DAM Notification: Asset " + status);
        email.setMsg(
            "Hello,\n\n"
            + "Your asset has been " + status.toLowerCase() + ".\n\n"
            + "Asset path: " + assetPath + "\n"
            + "Action: " + status + "\n\n"
            + "This notification was sent after administrative approval.\n"
        );

        gateway.send(email);

        LOG.info("Owner email submitted: action={}, asset={}",
                action, assetPath);
    }

    private String getOwnerEmail(String assetPath) throws Exception {

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(
            ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

        String metadataPath =
            assetPath + "/jcr:content/metadata";

        try (ResourceResolver resolver =
                resolverFactory.getServiceResourceResolver(authInfo)) {

            Resource metadata = resolver.getResource(metadataPath);

            if (metadata == null) {
                return null;
            }

            ValueMap properties = metadata.getValueMap();

            String email = properties.get(
                OWNER_EMAIL_PROPERTY, String.class);

            return email == null ? null : email.trim();
        }
    }

    private boolean isPlausibleEmail(String email) {
        return email != null
            && email.length() <= 254
            && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
