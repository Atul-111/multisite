package com.multisite.core.services.impl;

import com.multisite.core.services.FormPrefillService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configurable prefill provider for the project's employee Adaptive Form.
 * Replace this provider, or delegate from it, when values need to come from a
 * CRM or the authenticated user's profile.
 */
@Component(service = FormPrefillService.class)
@Designate(ocd = FormPrefillServiceImpl.Config.class)
public class FormPrefillServiceImpl implements FormPrefillService {

    private static final String EMPLOYEE_FORM = "employee";

    private String employeeId;
    private String employeeName;

    @ObjectClassDefinition(name = "Multisite - Adaptive Form Prefill Service")
    public @interface Config {

        @AttributeDefinition(name = "Employee ID", description = "Default value for the employee form's id field")
        String employeeId() default "";

        @AttributeDefinition(name = "Employee name", description = "Default value for the employee form's name field")
        String employeeName() default "";
    }

    @Activate
    protected void activate(Config config) {
        employeeId = config.employeeId();
        employeeName = config.employeeName();
    }

    @Override
    public Map<String, Object> getPrefillData(String formName) {
        if (!EMPLOYEE_FORM.equalsIgnoreCase(formName)) {
            return Collections.emptyMap();
        }

        Map<String, Object> values = new LinkedHashMap<>();
        addIfPresent(values, "id", employeeId);
        addIfPresent(values, "name", employeeName);
        return values;
    }

    private void addIfPresent(Map<String, Object> values, String fieldName, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.put(fieldName, value);
        }
    }
}
