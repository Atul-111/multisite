
package com.multisite.core.workflow;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;

@Component(service = StartAssetApprovalWorkflow.class)
public class StartAssetApprovalWorkflow {

    private static final Logger LOG =
        LoggerFactory.getLogger(StartAssetApprovalWorkflow.class);

    private static final String WORKFLOW_MODEL =
        "/var/workflow/models/dam-asset-publish-unpublish-approval";

    private static final String SUBSERVICE =
        "multisite-notification";

    @Reference
    private ResourceResolverFactory resolverFactory;

    public void start(String assetPath, String action) throws Exception {

        if (assetPath == null || assetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset path is required");
        }

        if (!"ACTIVATE".equals(action)
                && !"DEACTIVATE".equals(action)) {
            throw new IllegalArgumentException(
                "Unsupported action: " + action);
        }

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(
            ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

        try (ResourceResolver resolver =
                resolverFactory.getServiceResourceResolver(authInfo)) {

            WorkflowSession workflowSession =
                resolver.adaptTo(WorkflowSession.class);

            if (workflowSession == null) {
                throw new IllegalStateException(
                    "Unable to obtain WorkflowSession");
            }

            WorkflowModel model =
                workflowSession.getModel(WORKFLOW_MODEL);

            if (model == null) {
                throw new IllegalStateException(
                    "Workflow model not found: " + WORKFLOW_MODEL);
            }

            WorkflowData data =
                workflowSession.newWorkflowData("JCR_PATH", assetPath);

            data.getMetaDataMap().put("replicationAction", action);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("replicationAction", action);
            metadata.put("assetPath", assetPath);

            Workflow workflow =
                workflowSession.startWorkflow(model, data, metadata);

            LOG.info("Workflow {} started for {}",
                    workflow.getId(), assetPath);
        }
    }
}
