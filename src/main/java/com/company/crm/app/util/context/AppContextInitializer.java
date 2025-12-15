package com.company.crm.app.util.context;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class AppContextInitializer {

    @EventListener
    @Order(Integer.MIN_VALUE)
    public void onContextRefreshed(ContextRefreshedEvent event) {
        AppContext.setContext(event.getApplicationContext());
    }
}