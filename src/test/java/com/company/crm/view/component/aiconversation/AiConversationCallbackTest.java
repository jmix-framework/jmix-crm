package com.company.crm.view.component.aiconversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.util.extenstion.DataCleaner;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.testassist.UiTest;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test demonstrating callback pattern improvements with AiConversationComponent.
 * Shows the evolution from hacky to clean Jmix Function-based patterns.
 * TODO: Add @Supply/@Install support once we understand the exact Jmix framework requirements.
 */
@UiTest
@ExtendWith(DataCleaner.class)
public class AiConversationCallbackTest extends AbstractUiTest {

    @Autowired
    private ViewNavigators viewNavigators;

    @Test
    void test_functionBasedCallback_whenSetProgrammatically_customProcessorApplied() {
        // Given - Navigate to test view
        CallbackTestView view = navigateTo(CallbackTestView.class);

        // When - Set custom processor using Function-based API (clean Jmix pattern)
        view.getAiComponent().setMessageProcessor(userMessage ->
            "Function-based: " + userMessage.toUpperCase()
        );

        // Then - Component should use custom processor
        assertThat(view.getMessageCount()).isEqualTo(1); // Welcome message

        // When - Send message
        view.sendMessage("test");

        // Then - Should use the Function-based processor
        assertThat(view.getMessageCount()).isEqualTo(3);
        assertThat(view.getMessageText(2)).isEqualTo("Function-based: TEST");
        assertThat(view.getMessageAuthor(2)).isEqualTo("Assistant");
    }

    @Test
    void test_lambdaCallback_whenComplexLogic_behaviorWorks() {
        // Given
        CallbackTestView view = navigateTo(CallbackTestView.class);

        // When - Set complex processor with lambda (shows flexibility)
        view.getAiComponent().setMessageProcessor(userMessage -> {
            if (userMessage.toLowerCase().contains("hello")) {
                return "Lambda says: Greetings, " + userMessage + "!";
            } else {
                return "Lambda processed: " + userMessage.toLowerCase();
            }
        });

        // When - Send different messages
        view.sendMessage("Hello World");
        assertThat(view.getMessageText(2)).isEqualTo("Lambda says: Greetings, Hello World!");

        view.sendMessage("TEST");
        assertThat(view.getMessageText(4)).isEqualTo("Lambda processed: test");
    }

    // Helper method
    private CallbackTestView navigateTo(Class<CallbackTestView> viewClass) {
        viewNavigators.view(UiTestUtils.getCurrentView(), viewClass).navigate();
        return UiTestUtils.getCurrentView();
    }

    @Override
    protected boolean cleanDataAfterEach() {
        return true;
    }

    /**
     * Simple Test View that demonstrates improved Function-based callback patterns.
     * No complex @Install/@Supply for now - just clean programmatic API.
     */
    @Route(value = "test-callback-ai-conversation")
    @ViewController("CallbackTestView")
    @ViewDescriptor("test-callback-view.xml")
    public static class CallbackTestView extends StandardView {

        @ViewComponent
        private AiConversationComponent aiComponent;

        @Subscribe
        public void onReady(ReadyEvent event) {
            // Add welcome message for test expectations
            aiComponent.addWelcomeMessage("Welcome to callback test!");
        }

        // Test helper methods
        public AiConversationComponent getAiComponent() {
            return aiComponent;
        }

        public int getMessageCount() {
            var items = aiComponent.getMessageList().getItems();
            return items != null ? items.size() : 0;
        }

        public String getMessageText(int index) {
            var items = aiComponent.getMessageList().getItems();
            return index < items.size() ? items.get(index).getText() : null;
        }

        public String getMessageAuthor(int index) {
            var items = aiComponent.getMessageList().getItems();
            return index < items.size() ? items.get(index).getUserName() : null;
        }

        public void sendMessage(String message) {
            aiComponent.submitMessage(message);
        }
    }
}