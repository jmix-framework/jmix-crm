package com.company.crm.test.settings;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.settings.CrmSettingsService;
import com.company.crm.model.settings.CrmSettings;
import io.jmix.appsettings.AppSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CrmSettingsServiceTest extends AbstractServiceTest<CrmSettingsService> {

    @Autowired
    private AppSettings appSettings;

    @Test
    void defaultVatPercent_hasDefaultValue() {
        assertThat(service.getDefaultVatPercent()).isEqualByComparingTo("20");
    }

    @Test
    void defaultVatPercent_reflectsSavedSettings() {
        CrmSettings settings = service.loadSettings();
        settings.setDefaultVatPercent(new BigDecimal("15"));
        appSettings.save(settings);

        assertThat(service.getDefaultVatPercent()).isEqualByComparingTo("15");
    }
}
