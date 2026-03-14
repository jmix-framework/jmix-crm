package com.company.crm.ai.service;

import com.company.crm.AbstractTest;
import io.jmix.core.security.CurrentAuthentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "AI_ENABLED", matches = "true")
class SpringAiToolCallbackUserContextLLMTest extends AbstractTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private ChatClient chatClient;
    private WhoAmITool whoAmITool;

    @BeforeEach
    void setUp() {
        // given
        this.whoAmITool = new WhoAmITool(currentAuthentication);
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a strict test assistant.
                        Always call the whoAmI tool exactly once.
                        Return only the raw tool output with no extra text.
                        """)
                .build();
    }

    @Test
    void whoAmIToolCallbackRunsAsManagerUser() {
        // given
        String nonce = UUID.randomUUID().toString();
        whoAmITool.setNonce(nonce);

        // when
        String response = withManager(() -> chatClient.prompt()
                .user("Call whoAmI now and return the exact tool output.")
                .tools(whoAmITool)
                .call()
                .content());

        // then
        assertThat(response).contains("username=manager");
        assertThat(response).contains("nonce=" + nonce);
    }

    @Test
    void whoAmIToolCallbackRunsAsSupervisorUser() {
        // given
        String nonce = UUID.randomUUID().toString();
        whoAmITool.setNonce(nonce);

        // when
        String response = withSupervisor(() -> chatClient.prompt()
                .user("Call whoAmI now and return the exact tool output.")
                .tools(whoAmITool)
                .call()
                .content());

        // then
        assertThat(response).contains("username=supervisor");
        assertThat(response).contains("nonce=" + nonce);
    }

    private static final class WhoAmITool {

        private final CurrentAuthentication currentAuthentication;
        private String nonce;

        private WhoAmITool(CurrentAuthentication currentAuthentication) {
            this.currentAuthentication = currentAuthentication;
        }

        private void setNonce(String nonce) {
            this.nonce = nonce;
        }

        @Tool(description = "Returns the current authenticated username and a server-side nonce.")
        public String whoAmI() {
            return "username=" + currentAuthentication.getUser().getUsername() + ";nonce=" + nonce;
        }
    }
}
