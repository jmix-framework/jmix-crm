package com.company.crm.test.ai;

import com.company.crm.AbstractTest;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import io.jmix.dynmodelai.DynamicModelAgentService;
import io.jmix.dynmodelaiflowui.view.settings.AgentDynamicModelSettingsView;
import io.jmix.flowui.view.ViewRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicModelAgentIntegrationTest extends AbstractTest {

    @Autowired
    private DynamicModelAgentService agent;
    @Autowired
    private ViewRegistry viewRegistry;
    @Autowired
    private SpringAiLlmService dynamicModelLlm;
    @Autowired
    private OpenAiChatModel crmChatModel;

    @Test
    void agentPersistenceAndEditorAreAvailableAlongsideCrm() {
        systemAuthenticator.runWithSystem(() ->
                assertThat(agent.getConversations(PageRequest.of(0, 20))).isNotNull());
        assertThat(viewRegistry.getViewInfo("dynmod_Settings").getControllerClass())
                .isEqualTo(AgentDynamicModelSettingsView.class);
    }

    @Test
    void modelConfigurationDoesNotReplaceTheCrmAssistant() {
        assertThat(dynamicModelLlm.getChatModel()).isNotSameAs(crmChatModel);
        assertThat(dynamicModelLlm.getName()).isEqualTo(
                applicationContext.getEnvironment().getRequiredProperty("crm.dynmodel.model"));
        assertThat(applicationContext.getEnvironment().getProperty("jmix.dynmodel.ai.plan-approval-mode"))
                .isEqualTo("MANUAL");
    }
}
