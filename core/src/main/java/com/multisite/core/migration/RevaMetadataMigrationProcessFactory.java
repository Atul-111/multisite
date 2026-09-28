package com.multisite.core.migration;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import org.osgi.service.component.annotations.Component;

@Component(service = ProcessDefinitionFactory.class)
public class RevaMetadataMigrationProcessFactory
        extends ProcessDefinitionFactory<RevaMetadataMigrationProcess> {

    @Override
    public String getName() {
        return "Reva Metadata Migration";
    }

    @Override
    protected RevaMetadataMigrationProcess createProcessDefinitionInstance() {
        return new RevaMetadataMigrationProcess();
    }
}