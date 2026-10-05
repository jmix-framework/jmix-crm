package com.company.crm;

import com.company.crm.app.annotation.NotOnlineProfile;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;
import io.jmix.flowui.theme.aura.JmixAura;
import io.jmix.flowui.theme.lumo.JmixLumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Map;

@Push
@ColorScheme(ColorScheme.Value.LIGHT_DARK)
@JsModule("./src/theme/color-scheme-switching-support.js")
@PWA(name = "CRM", shortName = "CRM", offline = false)
@SpringBootApplication
public class CRMApplication implements AppShellConfigurator {

    // Demo-stand appearance: theme, colour scheme and direction from -Dstand.theme, -Dstand.color, -Dstand.direction.
    @Override
    public void configurePage(AppShellSettings settings) {
        String theme = System.getProperty("stand.theme", "aura");
        String color = System.getProperty("stand.color", "light");
        String direction = System.getProperty("stand.direction", "ltr");
        if (!java.util.Set.of("aura", "lumo").contains(theme)
                || !java.util.Set.of("light", "dark").contains(color)
                || !java.util.Set.of("ltr", "rtl").contains(direction)) {
            throw new IllegalArgumentException("Invalid stand appearance");
        }
        boolean aura = theme.equals("aura");
        settings.addLink(aura ? Aura.STYLESHEET : Lumo.STYLESHEET, Map.of("rel", "stylesheet"));
        settings.addLink(aura ? JmixAura.STYLESHEET : JmixLumo.STYLESHEET, Map.of("rel", "stylesheet"));
        settings.addLink("themes/" + theme + "/styles.css", Map.of("rel", "stylesheet"));
        settings.addInlineWithContents("localStorage.setItem('jmix.app.theme', '" + color + "');"
                + "document.documentElement.setAttribute('theme', '" + color + "');"
                + "document.documentElement.style.colorScheme='" + color + "';"
                + "document.documentElement.dir='" + direction + "';", Inline.Wrapping.JAVASCRIPT);
    }

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
