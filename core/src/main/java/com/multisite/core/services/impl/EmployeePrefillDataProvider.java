package com.multisite.core.services.impl;

import com.adobe.forms.common.service.ContentType;
import com.adobe.forms.common.service.DataOptions;
import com.adobe.forms.common.service.DataProvider;
import com.adobe.forms.common.service.FormsException;
import com.adobe.forms.common.service.PrefillData;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Native AEM Forms prefill provider for the employee Adaptive Form.
 * AEM Forms invokes this service while rendering a form that has selected it
 * as its Prefill Service; no browser-side request is required.
 */
@Component(service = DataProvider.class, immediate = true)
@Designate(ocd = EmployeePrefillDataProvider.Config.class)
public class EmployeePrefillDataProvider implements DataProvider {

    private String employeeId;
    private String employeeName;

    @ObjectClassDefinition(name = "Multisite - Employee Native Prefill Service")
    public @interface Config {

        @AttributeDefinition(name = "Employee ID")
        String employeeId() default "22012019014";

        @AttributeDefinition(name = "Employee name")
        String employeeName() default "Manoj Maurya";
    }

    @Activate
    protected void activate(Config config) {
        employeeId = config.employeeId();
        employeeName = config.employeeName();
    }

    @Override
    public PrefillData getPrefillData(DataOptions options) throws FormsException {
        try {
            JSONObject values = new JSONObject();
            values.put("id", employeeId);
            values.put("name", employeeName);

            return new PrefillData(
                    new ByteArrayInputStream(values.toString().getBytes(StandardCharsets.UTF_8)),
                    ContentType.JSON);
        } catch (Exception e) {
            throw new FormsException("Unable to create employee prefill data", e);
        }
    }

    @Override
    public String getServiceName() {
        return "Employee Native Prefill Service";
    }

    @Override
    public String getServiceDescription() {
        return "Prefills employee ID and name from the Multisite configuration";
    }
}
