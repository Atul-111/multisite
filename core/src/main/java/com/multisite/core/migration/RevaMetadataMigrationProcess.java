package com.multisite.core.migration;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.PersistenceException;

import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class RevaMetadataMigrationProcess extends ProcessDefinition {

    private static final String REVA_ID_PROPERTY = "revaDisplayId";
    private static final String REVA_LINK_PROPERTY = "revaLink";

    @FormField(
            name = "Asset Root",
            description = "Root path containing PA assets",
            required = true
    )
    private String assetRoot = "/content/dam";

    @FormField(
            name = "Batch Size",
            description = "Number of assets to inspect",
            required = true
    )
    private int batchSize = 10;

    @FormField(
            name = "Dry Run",
            description = "If true, no metadata will be modified"
    )
    private boolean dryRun = true;

    public enum ReportColumns {
        PATH,
        REVA_ID,
        REVA_LINK,
        STATUS
    }

    private final List<EnumMap<ReportColumns, String>> reportData =
            Collections.synchronizedList(new ArrayList<>());

    private int processedCount = 0;
    private int revaIdFoundCount = 0;
    private int revaIdMissingCount = 0;

    @Override
    public void init() {
        // No initialization required.
    }

    @Override
public void buildProcess(
        ProcessInstance instance,
        ResourceResolver resourceResolver)
        throws LoginException, RepositoryException {

    instance.defineAction(
            "Scan Reva Metadata",
            resourceResolver,
            this::scanAssets
    );
}

    public void handleException(Exception e) {
        // Handle exceptions thrown by actions
    }

    private void scanAssets(ActionManager manager) {

        manager.deferredWithResolver(rr -> {

            Resource root = rr.getResource(assetRoot);

            if (root == null) {
                record(
                        assetRoot,
                        "",
                        "",
                        "ASSET_ROOT_NOT_FOUND"
                );
                return;
            }

            int[] count = {0};

            root.getResourceResolver()
                    .findResources(
                            "SELECT * FROM [dam:Asset] AS asset " +
                            "WHERE ISDESCENDANTNODE(asset, '" +
                            assetRoot +
                            "')",
                            "JCR-SQL2"
                    )
                    .forEachRemaining(asset -> {

                        if (count[0] >= batchSize) {
                            return;
                        }

                        count[0]++;
                        processedCount++;

                        Resource metadata =
                                asset.getChild("jcr:content/metadata");

                        if (metadata == null) {

                            record(
                                    asset.getPath(),
                                    "",
                                    "",
                                    "METADATA_NODE_MISSING"
                            );

                            return;
                        }

                        String revaId =
                                metadata.getValueMap().get(
                                        REVA_ID_PROPERTY,
                                        String.class
                                );

                        String revaLink =
                                metadata.getValueMap().get(
                                        REVA_LINK_PROPERTY,
                                        String.class
                                );

                        if (revaId == null ||
                                revaId.trim().isEmpty()) {

                            revaIdMissingCount++;

                            record(
                                    asset.getPath(),
                                    "",
                                    revaLink,
                                    "REVA_ID_MISSING"
                            );

                        } else {

                            revaIdFoundCount++;

                            record(
                                    asset.getPath(),
                                    revaId,
                                    revaLink,
                                    "REVA_ID_FOUND"
                            );
                        }
                    });
        });
    }

    private void record(
            String path,
            String revaId,
            String revaLink,
            String status) {

        EnumMap<ReportColumns, String> row =
                new EnumMap<>(ReportColumns.class);

        row.put(ReportColumns.PATH, path);
        row.put(ReportColumns.REVA_ID, revaId);
        row.put(ReportColumns.REVA_LINK, revaLink);
        row.put(ReportColumns.STATUS, status);

        reportData.add(row);
    }

    @Override
    public void storeReport(
            ProcessInstance instance,
            ResourceResolver resourceResolver)
            throws RepositoryException, PersistenceException {

        GenericBlobReport report =
                new GenericBlobReport();

        report.setName(
                "Reva Metadata Migration Report"
        );

        report.setRows(
                reportData,
                ReportColumns.class
        );

        report.persist(
                resourceResolver,
                instance.getPath() +
                        "/jcr:content/report"
        );
    }
}