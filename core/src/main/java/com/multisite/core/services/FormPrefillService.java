package com.multisite.core.services;

import java.util.Map;

/** Supplies initial values for Adaptive Forms. */
public interface FormPrefillService {

    /**
     * Returns values keyed by the Adaptive Form field name.
     *
     * @param formName logical name of the form being loaded
     * @return values to apply to the form; never {@code null}
     */
    Map<String, Object> getPrefillData(String formName);
}
