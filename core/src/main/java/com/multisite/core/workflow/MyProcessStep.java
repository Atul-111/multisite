package com.multisite.core.workflow;

import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.metadata.MetaDataMap;

import org.osgi.service.component.annotations.Component;

@Component(
    service = WorkflowProcess.class,
    property = { "process.label=My Custom Process Step" }
)
public class MyProcessStep implements WorkflowProcess {

    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) {

        String payloadPath = item.getWorkflowData().getPayload().toString();

        System.out.println("Workflow triggered on: " + payloadPath);

        // Example logic
        if (payloadPath.contains("/content")) {
            System.out.println("Valid content path detected");
        }
    }
}