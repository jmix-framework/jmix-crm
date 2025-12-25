package com.company.crm.util.extenstion;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public final class ExtensionUtils {

    public static <BEAN> BEAN getBean(ExtensionContext context, Class<BEAN> beanType) {
        return SpringExtension.getApplicationContext(context).getBean(beanType);
    }

    private ExtensionUtils() {
    }
}
