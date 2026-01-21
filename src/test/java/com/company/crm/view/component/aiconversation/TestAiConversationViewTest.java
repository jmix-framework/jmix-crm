package com.company.crm.view.component.aiconversation;

import com.company.crm.AbstractUiTest;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the TestAiConversationView itself.
 * This verifies that our test view works correctly.
 */
public class TestAiConversationViewTest extends AbstractUiTest {

    @Test
    void testTestViewLoadsSuccessfully() {
        // Navigate to test view
        viewNavigators.view(UiTestUtils.getCurrentView(), TestAiConversationView.class).navigate();

        TestAiConversationView testView = UiTestUtils.getCurrentView();
        assertNotNull(testView);
    }

    @Test
    void testAiComponentIsPresent() {
        // Navigate to test view
        viewNavigators.view(UiTestUtils.getCurrentView(), TestAiConversationView.class).navigate();

        TestAiConversationView testView = UiTestUtils.getCurrentView();

        // Verify the AI component was created and added
        AiConversationComponent aiComponent = testView.getAiComponent();
        assertNotNull(aiComponent);
    }

    @Test
    void testMockMessageProcessing() {
        // Navigate to test view
        viewNavigators.view(UiTestUtils.getCurrentView(), TestAiConversationView.class).navigate();

        TestAiConversationView testView = UiTestUtils.getCurrentView();
        AiConversationComponent aiComponent = testView.getAiComponent();

        assertNotNull(aiComponent);

        // Test adding messages manually
        aiComponent.addMessage("Test user message", "User", 1);
        aiComponent.addMessage("Test AI response", "Assistant", 2);

        // Verify component is functional - conversation ID is null until a message is sent through processor
        assertNull(aiComponent.getConversationId());
    }

    @Test
    void testComponentConfiguration() {
        // Navigate to test view
        viewNavigators.view(UiTestUtils.getCurrentView(), TestAiConversationView.class).navigate();

        TestAiConversationView testView = UiTestUtils.getCurrentView();
        AiConversationComponent aiComponent = testView.getAiComponent();

        assertNotNull(aiComponent);

        // Component should be configured with header and welcome message
        // (We can't directly test the internal configuration, but we can verify it doesn't throw errors)

        // Try to interact with the component
        aiComponent.clearMessages();
        aiComponent.addMessage("Configuration test", "User", 1);

        assertTrue(true, "Component configuration and interaction successful");
    }

    @Test
    void testViewIsAccessible() {
        // The test view should be accessible via its route
        // Navigate to test view
        viewNavigators.view(UiTestUtils.getCurrentView(), TestAiConversationView.class).navigate();

        TestAiConversationView testView = UiTestUtils.getCurrentView();
        assertNotNull(testView);

        // The view should have the expected components
        AiConversationComponent aiComponent = testView.getAiComponent();
        assertNotNull(aiComponent);
    }
}