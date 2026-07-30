package com.multisite.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue; // Ye import add karein
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public interface TeamModel {

    @ChildResource
    List<TeamMember> getMembers();

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    interface TeamMember {
        
        @ValueMapValue // Ye annotation zaroori hai
        String getName();

        @ValueMapValue // Ye annotation zaroori hai
        String getRole();
    }
}