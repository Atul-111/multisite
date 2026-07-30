package com.multisite.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ExternalApiModel {

    @ValueMapValue
    private String apiUrl; // Dialog se URL lene ke liye

    private String apiData;

    @PostConstruct
    protected void init() {
        // Agar dialog mein URL nahi hai toh default use karein
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://jsonplaceholder.typicode.com/todos/1";
        }

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                apiData = response.toString();
                reader.close();
            } else {
                apiData = "API Error: Status Code " + conn.getResponseCode();
            }
        } catch (Exception e) {
            apiData = "Exception: " + e.getMessage();
        }
    }

    public String getApiData() {
        return apiData;
    }
}