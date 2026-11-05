package com.company.crm.test.dynmodel;

import com.company.crm.AbstractUiTest;
import io.jmix.dynmodelflowui.view.DynamicModelSettingsEditorMode;
import io.jmix.dynmodelflowui.view.DynamicModelSettingsView;
import io.jmix.flowui.menu.MenuConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicModelSettingsViewTest extends AbstractUiTest {

    @Autowired
    private MenuConfig menuConfig;

    @Test
    void adminMenu_opensDynamicModelCodeEditor() {
        var administration = menuConfig.getRootItems().stream()
                .filter(item -> "administration".equals(item.getId()))
                .findFirst().orElseThrow();
        assertThat(administration.getChildren()).anySatisfy(item -> {
            assertThat(item.getId()).isEqualTo("dynamic_model");
            assertThat(item.getView()).isEqualTo("dynmod_Settings");
        });

        viewTestSupport.navigateTo(DynamicModelSettingsView.class);
        assertCurrentView(DynamicModelSettingsView.class);
        viewTestSupport.setComponentValue("editorModeField", DynamicModelSettingsEditorMode.CODE);
        assertThat(viewTestSupport.isVisible("contentField")).isTrue();
    }
}
