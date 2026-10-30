
package com.multisite.core.jobs;

import com.multisite.core.workflow.StartAssetApprovalWorkflow;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = JobConsumer.class,
    property = {
        JobConsumer.PROPERTY_TOPICS
            + "=multisite/dam/asset-approval"
    }
)
public class AssetApprovalJobConsumer implements JobConsumer {

    private static final Logger LOG =
        LoggerFactory.getLogger(AssetApprovalJobConsumer.class);

    @Reference
    private StartAssetApprovalWorkflow workflowStarter;

    @Override
    public JobResult process(Job job) {

        String assetPath =
            job.getProperty("assetPath", String.class);

        String action =
            job.getProperty("action", String.class);

        // Validate the job payload.
        if (assetPath == null || assetPath.trim().isEmpty()) {
            LOG.error("Asset approval job has no assetPath");
            return JobResult.CANCEL;
        }

        if (!"ACTIVATE".equals(action)
                && !"DEACTIVATE".equals(action)) {
            LOG.error(
                "Invalid replication action '{}' for asset '{}'",
                action,
                assetPath
            );
            return JobResult.CANCEL;
        }

        try {
            // Start the AEM approval workflow.
            workflowStarter.start(assetPath, action);

            LOG.info(
                "Started asset approval workflow: action={}, path={}",
                action,
                assetPath
            );

            return JobResult.OK;

        } catch (Exception e) {
            LOG.error(
                "Unable to start approval workflow for asset: {}",
                assetPath,
                e
            );

            return JobResult.FAILED;
        }
    }
}
