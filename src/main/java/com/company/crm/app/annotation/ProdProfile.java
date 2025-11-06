package com.company.crm.app.annotation;

import com.company.crm.app.util.constant.CrmConstants.SpringProfiles;
import org.springframework.context.annotation.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Profile(SpringProfiles.PROD)
public @interface ProdProfile {
}