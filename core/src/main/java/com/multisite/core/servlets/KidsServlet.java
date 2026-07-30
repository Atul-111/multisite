package com.multisite.core.servlets;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import org.osgi.service.component.annotations.Component;

import static org.apache.sling.api.servlets.ServletResolverConstants.*;

@Component(
    service = Servlet.class,
    property = {
        SLING_SERVLET_PATHS + "=/bin/techtalkwithritesh/kids",
        SLING_SERVLET_METHODS + "=GET"
    }
)
public class KidsServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        String jsonResponse =
                "[{\"id\":1,\"name\":\"Rahul\",\"age\":12}," +
                "{\"id\":2,\"name\":\"Harsh\",\"age\":14}]";

        response.getWriter().write(jsonResponse);
    }
}