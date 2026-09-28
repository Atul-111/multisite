package com.multisite.core.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FormPrefillServiceImplTest {

    @Test
    void returnsConfiguredEmployeeValues() {
        FormPrefillServiceImpl service = new FormPrefillServiceImpl();
        FormPrefillServiceImpl.Config config = Mockito.mock(FormPrefillServiceImpl.Config.class);
        Mockito.when(config.employeeId()).thenReturn("123");
        Mockito.when(config.employeeName()).thenReturn("Jane Doe");
        service.activate(config);

        Map<String, Object> values = service.getPrefillData("employee");

        assertEquals("123", values.get("id"));
        assertEquals("Jane Doe", values.get("name"));
    }

    @Test
    void returnsNoValuesForUnknownForm() {
        FormPrefillServiceImpl service = new FormPrefillServiceImpl();
        FormPrefillServiceImpl.Config config = Mockito.mock(FormPrefillServiceImpl.Config.class);
        service.activate(config);

        assertTrue(service.getPrefillData("unknown").isEmpty());
    }
}
