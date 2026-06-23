package com.company.crm.test.ai.tool;

import com.company.crm.AbstractTest;
import com.company.crm.ai.tool.CrmAvailableEntityFilter;
import io.jmix.aitools.dataload.introspection.AvailableEntityFilter;
import io.jmix.aitools.dataload.tool.DataLoadAiTool;
import io.jmix.aitools.tool.AiToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AiToolRegistryWiringTest extends AbstractTest {

    @Autowired
    private AiToolRegistry aiToolRegistry;

    @Autowired
    private AvailableEntityFilter availableEntityFilter;

    @Test
    void keptCrmToolsAreRegistered() {
        assertThat(aiToolRegistry.findByName("getAvailableReports")).isPresent();
        assertThat(aiToolRegistry.findByName("getReportsByCodes")).isPresent();
        assertThat(aiToolRegistry.findByName("runReport")).isPresent();
        assertThat(aiToolRegistry.findByName("getAvailableRoutes")).isPresent();
    }

    @Test
    void addonDataLoadToolsAreRegistered() {
        assertThat(aiToolRegistry.findByMarker(DataLoadAiTool.class)).isNotEmpty();
        assertThat(aiToolRegistry.getAllCallbacks()).isNotEmpty();
    }

    @Test
    void crmAllowlistFilterOverridesAddonDefault() {
        assertThat(availableEntityFilter).isInstanceOf(CrmAvailableEntityFilter.class);
    }
}
