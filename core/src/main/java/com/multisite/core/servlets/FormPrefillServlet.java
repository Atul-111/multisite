package com.multisite.core.servlets;

import com.multisite.core.services.FormPrefillService;
import java.io.IOException;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

/** Exposes form-prefill data to the Adaptive Form client library. */
@Component(service = Servlet.class, property = {
        SLING_SERVLET_PATHS + "=/bin/multisite/forms/prefill",
        SLING_SERVLET_METHODS + "=GET"
})
public class FormPrefillServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_FORM = "employee";

    @Reference
    private FormPrefillService formPrefillService;

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {
        String formName = request.getParameter("form");
        if (formName == null || formName.trim().isEmpty()) {
            formName = DEFAULT_FORM;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new JSONObject(
                formPrefillService.getPrefillData(formName)).toString());
    }
}
