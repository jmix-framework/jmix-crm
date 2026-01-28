package com.company.crm.app.service.settings;

import com.company.crm.model.settings.CrmSettings;
import io.jmix.appsettings.AppSettings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CrmSettingsService {

    private final AppSettings appSettings;

    public CrmSettingsService(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    public CrmSettings loadSettings() {
        return appSettings.load(CrmSettings.class);
    }

    public BigDecimal getDefaultVatPercent() {
        return loadSettings().getDefaultVatPercent();
    }
}
