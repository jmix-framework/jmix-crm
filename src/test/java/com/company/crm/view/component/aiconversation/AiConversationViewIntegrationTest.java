package com.company.crm.view.component.aiconversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.util.extenstion.DataCleaner;
import io.jmix.flowui.testassist.UiTest;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for AiConversationComponent via TestAiConversationView.
 * Tests the component by opening the actual view and interacting with it using Jmix test patterns.
 */
@UiTest
@ExtendWith(DataCleaner.class)
public class AiConversationViewIntegrationTest extends AbstractUiTest {

    @Test
    public void test_viewOpensSuccessfully() {
        // When
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // Then
        assertThat(view).isNotNull();
        assertThat(view.getAiComponent()).isNotNull();
    }

    @Test
    void test_xmlConfiguration_whenViewOpens_componentInitializedWithWelcomeMessage() {
        // When
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // Then - Should have welcome message from XML config using view's helper methods
        assertThat(view.getMessageCount()).isEqualTo(1);
        assertThat(view.getMessageText(0)).isEqualTo("Hello! This is a test chat.");
        assertThat(view.getMessageAuthor(0)).isEqualTo("Assistant");
    }

    @Test
    void test_messageSending_whenSendMessage_userMessageAndAiResponseAdded() {
        // Given
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // Initially should have welcome message
        assertThat(view.getMessageCount()).isEqualTo(1);

        // When - Send a message that doesn't trigger specific AI response
        view.sendMessage("Random message");

        // Then - Should have welcome + user message + generic AI response
        assertThat(view.getMessageCount()).isEqualTo(3);
        assertThat(view.getMessageText(1)).isEqualTo("Random message");
        assertThat(view.getMessageAuthor(1)).isEqualTo("User");
        assertThat(view.getMessageAuthor(2)).isEqualTo("Assistant");
    }

    @Test
    void test_messageSending_whenSendHelloMessage_specificAiResponseIsGenerated() {
        // Given
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // When
        view.sendMessage("hello");

        // Then - Should get specific "hello" response
        assertThat(view.getMessageCount()).isEqualTo(3);
        assertThat(view.getMessageText(1)).isEqualTo("hello");
        assertThat(view.getMessageAuthor(1)).isEqualTo("User");
        assertThat(view.getMessageText(2)).isEqualTo("Hello! How can I help you today?");
        assertThat(view.getMessageAuthor(2)).isEqualTo("Assistant");
    }

    @Test
    void test_messageSending_whenSendTestMessage_testResponseIsGenerated() {
        // Given
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // When
        view.sendMessage("test");

        // Then - Should get test-specific response
        assertThat(view.getMessageCount()).isEqualTo(3);
        String response = view.getMessageText(2);
        assertThat(response)
            .contains("This is a test response")
            .contains("test");
    }

    @Test
    void test_errorHandling_whenSendErrorMessage_errorResponseIsShown() {
        // Given
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // When - Send message that triggers error
        view.sendMessage("error");

        // Then - Should show error message
        assertThat(view.getMessageCount()).isEqualTo(3);
        assertThat(view.getMessageText(2))
            .isEqualTo("Sorry, I'm having trouble connecting right now. Please try again later.");
        assertThat(view.getMessageAuthor(2)).isEqualTo("Assistant");
    }

    @Test
    void test_deterministicTesting_whenDeterministicProcessorSet_fixedResponseIsReturned() {
        // Given
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // Set deterministic processor
        view.setDeterministicMessageProcessor("Fixed test response");

        // When
        view.sendMessage("any message");

        // Then - Should get the fixed response
        assertThat(view.getMessageCount()).isEqualTo(3);
        assertThat(view.getMessageText(2)).isEqualTo("Fixed test response");
        assertThat(view.getMessageAuthor(2)).isEqualTo("Assistant");
    }

    @Test
    void test_advancedCallback_whenCustomProcessorProviderSet_dynamicResponseIsReturned() {
        // Given
        TestAiConversationView view = navigateTo(TestAiConversationView.class);

        // Set custom processor provider using Jmix Function-based callback pattern
        view.setMessageProcessorProvider(userMessage ->
            "Echo: " + userMessage.toUpperCase() + " [processed by custom provider]"
        );

        // When
        view.sendMessage("hello world");

        // Then - Should get the dynamically processed response
        assertThat(view.getMessageCount()).isEqualTo(3);
        assertThat(view.getMessageText(2)).isEqualTo("Echo: HELLO WORLD [processed by custom provider]");
        assertThat(view.getMessageAuthor(2)).isEqualTo("Assistant");
    }

    // Helper methods following Jmix test patterns

    private TestAiConversationView navigateTo(Class<TestAiConversationView> viewClass) {
        viewNavigators.view(UiTestUtils.getCurrentView(), viewClass).navigate();
        return UiTestUtils.getCurrentView();
    }

    @Override
    protected boolean cleanDataAfterEach() {
        return true;
    }
}