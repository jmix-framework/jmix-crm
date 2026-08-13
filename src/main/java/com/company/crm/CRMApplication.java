package com.company.crm;

import com.company.crm.app.annotation.NotOnlineProfile;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;
import io.jmix.flowui.theme.aura.JmixAura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Push
@ColorScheme(ColorScheme.Value.LIGHT_DARK)
@StyleSheet(Aura.STYLESHEET)
@StyleSheet(JmixAura.STYLESHEET)
@StyleSheet("themes/aura/styles.css")
// JST-5452 case 1: flat stylesheet path from the crm-theme dependency (Vaadin 25 layout).
@StyleSheet("cobalt/master.css")
// JST-5452 case 2: reusable theme folder from the crm-theme dependency. Uncomment to check.
// @StyleSheet("themes/cobalt/styles.css")
// JST-5452 case 3: legacy standalone reusable theme. Comment out every @StyleSheet above first.
// @Theme("cobalt")
// JST-5452 case 4: project theme with cobalt as its dependency parent.
// Comment out every @StyleSheet above first.
// @Theme("crm-preview")
@JsModule("./src/theme/color-scheme-switching-support.js")
@PWA(name = "CRM", shortName = "CRM", offline = false)
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
