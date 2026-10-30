
package com.multisite.core.listeners;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationEvent;

import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "="
            + ReplicationEvent.EVENT_TOPIC
    }
)
public class AssetReplicationEventListener implements EventHandler {

    private static final Logger LOG =
        LoggerFactory.getLogger(AssetReplicationEventListener.class);

    private static final String DAM_ROOT =
        "/content/dam/multisite/testing";

    private static final String JOB_TOPIC =
        "multisite/dam/asset-approval";

    @Reference
    private JobManager jobManager;

    @Override
    public void handleEvent(Event event) {
        ReplicationEvent replicationEvent =
            ReplicationEvent.fromEvent(event);

        if (replicationEvent == null
                || replicationEvent.getReplicationAction() == null) {
            return;
        }

        ReplicationAction action =
            replicationEvent.getReplicationAction();

        String path = action.getPath();
        ReplicationActionType type = action.getType();

        if (path == null
                || !(path.equals(DAM_ROOT)
                || path.startsWith(DAM_ROOT + "/"))) {
            return;
        }

        if (type != ReplicationActionType.ACTIVATE
                && type != ReplicationActionType.DEACTIVATE) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("assetPath", path);
        properties.put("action", type.name());
        properties.put("initiatedBy", action.getUserId());

        try {
            jobManager.addJob(JOB_TOPIC, properties);
            LOG.info("Queued approval job: action={}, path={}",
                    type.name(), path);
        } catch (RuntimeException e) {
            LOG.error("Unable to queue approval job for {}", path, e);
        }
    }
}
