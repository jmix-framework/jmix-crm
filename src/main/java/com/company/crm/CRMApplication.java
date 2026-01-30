package com.company.crm;

import com.company.crm.app.annotation.NotOnlineProfile;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Push
@Theme(value = "crm")
@PWA(name = "CRM", shortName = "CRM", offline = false)
@JsModule("./src/theme/color-scheme-switching-support.js")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@SpringBootApplication
public class CRMApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(CRMApplication.class, args);
    }

    @Bean
    @Primary
    @NotOnlineProfile
    @ConfigurationProperties("main.datasource")
    DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @NotOnlineProfile
    @ConfigurationProperties("main.datasource.hikari")
    DataSource dataSource(final DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
}
