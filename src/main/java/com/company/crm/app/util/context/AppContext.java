package com.company.crm.app.util.context;

import org.springframework.context.ApplicationContext;

public final class AppContext {

    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    private static ApplicationContext context;

    static void setContext(ApplicationContext context) {
        AppContext.context = context;
    }
}
