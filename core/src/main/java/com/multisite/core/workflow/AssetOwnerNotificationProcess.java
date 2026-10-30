
package com.multisite.core.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.metadata.MetaDataMap;

import com.multisite.core.services.AssetNotificationService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Send Approved DAM Asset Notification"
    }
)
public class AssetOwnerNotificationProcess implements WorkflowProcess {

    private static final Logger LOG =
        LoggerFactory.getLogger(AssetOwnerNotificationProcess.class);

    @Reference
    private AssetNotificationService notificationService;

    @Override
    public void execute(
            WorkItem workItem,
            WorkflowSession workflowSession,
            MetaDataMap args) throws WorkflowException {

        Object payload = workItem.getWorkflowData().getPayload();

        if (!(payload instanceof String)) {
            throw new WorkflowException(
                "Workflow payload must be an asset path");
        }

        String assetPath = (String) payload;

        String action = workItem.getWorkflowData()
            .getMetaDataMap()
            .get("replicationAction", String.class);

        if (!"ACTIVATE".equals(action)
                && !"DEACTIVATE".equals(action)) {
            throw new WorkflowException(
                "Invalid replication action for " + assetPath);
        }

        try {
            notificationService.sendNotification(assetPath, action);

            LOG.info("Approved notification processed: action={}, path={}",
                    action, assetPath);
        } catch (Exception e) {
            LOG.error("Owner notification failed for {}", assetPath, e);
            throw new WorkflowException(
                "Unable to send owner notification for " + assetPath, e);
        }
    }
}
