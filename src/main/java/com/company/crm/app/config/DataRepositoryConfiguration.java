package com.company.crm.app.config;

import com.company.crm.CRMApplication;
import io.jmix.core.repository.EnableJmixDataRepositories;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableJmixDataRepositories(basePackageClasses = CRMApplication.class)
public class DataRepositoryConfiguration {
}
