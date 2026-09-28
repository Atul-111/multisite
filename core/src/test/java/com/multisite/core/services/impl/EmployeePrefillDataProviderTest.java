package com.multisite.core.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.adobe.forms.common.service.ContentType;
import com.adobe.forms.common.service.PrefillData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EmployeePrefillDataProviderTest {

    @Test
    void returnsEmployeeValuesAsJsonPrefillData() throws Exception {
        EmployeePrefillDataProvider provider = new EmployeePrefillDataProvider();
        EmployeePrefillDataProvider.Config config =
                Mockito.mock(EmployeePrefillDataProvider.Config.class);
        Mockito.when(config.employeeId()).thenReturn("22012019014");
        Mockito.when(config.employeeName()).thenReturn("Manoj Maurya");
        provider.activate(config);

        PrefillData result = provider.getPrefillData(null);

        assertEquals(ContentType.JSON, result.getContentType());
        assertEquals(true, result.getInputStream().available() > 0);
    }
}
