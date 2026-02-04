package com.company.crm.app.listener;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HealthCheckServiceInitListener implements VaadinServiceInitListener {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckServiceInitListener.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        // health check handling
        event.addRequestHandler(this::handleRequest);
    }

    private boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) {
        if ("/healthcheck".equals(request.getPathInfo())) {

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            try {
                response.getWriter().write("<h2>This is fine</h2>");
            } catch (IOException e) {
                log.error("Error writing health check response", e);
                return false;
            }

            return true;
        }

        return false;
    }
}
