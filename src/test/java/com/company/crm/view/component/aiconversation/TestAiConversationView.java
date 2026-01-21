package com.company.crm.view.component.aiconversation;

import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

/**
 * Test view for testing the AiConversationComponent in isolation.
 * This view is only for testing purposes and not part of the main application.
 */
@Route(value = "test-ai-conversation", layout = MainView.class)
@ViewController(id = "TestAiConversation")
@ViewDescriptor(path = "test-ai-conversation-view.xml")
public class TestAiConversationView extends StandardView {

    @ViewComponent
    private AiConversationComponent aiComponent;

    @Subscribe
    public void onReady(ReadyEvent event) {
        setupTestAiConversationComponent();
    }

    private void setupTestAiConversationComponent() {
        // The AI component is now injected from XML via @ViewComponent
        // XML configuration (showHeader, headerTitle, welcomeMessage) is already applied by ComponentLoader
        // We only need to provide the MessageProcessor using the clean Function pattern

        aiComponent.setMessageProcessor(this::generateTestResponse);
    }

    /**
     * Generates test responses based on user input - now used directly with Function pattern.
     */

    private String generateTestResponse(String userMessage) {
        // Simple test responses based on user input
        if (userMessage.toLowerCase().contains("hello")) {
            return "Hello! How can I help you today?";
        } else if (userMessage.toLowerCase().contains("test")) {
            return "This is a test response. Your message was: \"" + userMessage + "\"";
        } else if (userMessage.toLowerCase().contains("error")) {
            throw new RuntimeException("Test error: This is intentional for testing error handling");
        } else {
            return "I received your message: \"" + userMessage + "\". This is a mock AI response for testing purposes.";
        }
    }

    /**
     * Public getter for testing purposes.
     */
    public AiConversationComponent getAiComponent() {
        return aiComponent;
    }

    // Test helper methods for the view

    /**
     * Get the current number of messages.
     */
    public int getMessageCount() {
        var items = aiComponent.getMessageList().getItems();
        return items != null ? items.size() : 0;
    }

    /**
     * Get message at specific index.
     */
    public String getMessageText(int index) {
        var items = aiComponent.getMessageList().getItems();
        return index < items.size() ? items.get(index).getText() : null;
    }

    /**
     * Get message author at specific index.
     */
    public String getMessageAuthor(int index) {
        var items = aiComponent.getMessageList().getItems();
        return index < items.size() ? items.get(index).getUserName() : null;
    }


    /**
     * Delegate method to send a message for testing.
     * Uses the real AiConversationComponent.submitMessage() method.
     */
    public void sendMessage(String message) {
        aiComponent.submitMessage(message);
    }

    /**
     * Sets a deterministic message processor for testing.
     * Uses the clean Jmix Function-based callback pattern.
     */
    public void setDeterministicMessageProcessor(String fixedResponse) {
        aiComponent.setMessageProcessor(userMessage -> fixedResponse);
    }

    /**
     * Sets a custom message processor provider for advanced testing scenarios.
     * Following Jmix Function-based callback pattern.
     */
    public void setMessageProcessorProvider(java.util.function.Function<String, String> provider) {
        aiComponent.setMessageProcessor(provider);
    }
}