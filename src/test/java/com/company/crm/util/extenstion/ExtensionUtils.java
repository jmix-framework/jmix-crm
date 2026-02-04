package com.company.crm.util.extenstion;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.Annotation;

final class ExtensionUtils {

    static <BEAN> BEAN getBean(ExtensionContext context, Class<BEAN> beanType) {
        return SpringExtension.getApplicationContext(context).getBean(beanType);
    }

    static AuthenticatedAs createAuthenticatedAs(String username) {
        return new AuthenticatedAs() {
            @Override
            public String value() {
                return username;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return AuthenticatedAs.class;
            }
        };
    }

    private ExtensionUtils() {
    }
}
