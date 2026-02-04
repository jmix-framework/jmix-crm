package com.company.crm.util.extenstion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.company.crm.util.extenstion.ExtensionUtils.createAuthenticatedAs;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedAs {

    String value();

    String ADMIN_USERNAME = "admin";

    AuthenticatedAs ADMIN = createAuthenticatedAs(ADMIN_USERNAME);
}
