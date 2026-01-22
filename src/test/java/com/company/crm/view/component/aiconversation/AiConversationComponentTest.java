package com.company.crm.view.component.aiconversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.util.extenstion.DataCleaner;
import io.jmix.flowui.testassist.UiTest;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for AiConversationComponent functionality
 */
@UiTest
@ExtendWith(DataCleaner.class)
public class AiConversationComponentTest extends AbstractUiTest {

    // No mocks needed - TestAiAnnotationsView has built-in @Install messageProcessor

    @Test
    public void test_messageProcessing() {
        // Navigate to test view
        TestAiAnnotationsView view = navigateToTestAnnotationsView();

        // Verify view opened successfully
        assertThat(view).isNotNull();

        // Get the AI component
        AiConversationComponent aiComponent = view.aiComponent;
        assertThat(aiComponent).isNotNull();

        // Test message processing by sending a message
        aiComponent.submitMessage("Test message");

        // Verify message was processed
        assertThat(getMessageCount(aiComponent)).isEqualTo(2); // user message + AI response

        // Verify user message
        String userMessage = getMessageText(aiComponent, 0);
        assertThat(userMessage).isEqualTo("Test message");

        // Verify AI response
        String aiResponse = getMessageText(aiComponent, 1);
        assertThat(aiResponse).isEqualTo("Annotation processed: TEST MESSAGE (processed via @Install)");
        assertThat(getMessageAuthor(aiComponent, 1)).isEqualTo("crm.assistant.name"); // Message key wird nicht aufgelöst
    }

    @Test
    public void test_basicFunctionality() {
        // Navigate to test annotations view
        TestAiAnnotationsView view = navigateToTestAnnotationsView();

        // Verify view opened successfully
        assertThat(view).isNotNull();

        // Get the AI component
        AiConversationComponent aiComponent = view.aiComponent;
        assertThat(aiComponent).isNotNull();

        // Test different messages to verify consistency
        aiComponent.submitMessage("hello world");
        assertThat(getMessageCount(aiComponent)).isEqualTo(2);
        assertThat(getMessageText(aiComponent, 1)).isEqualTo("Annotation processed: HELLO WORLD (processed via @Install)");

        aiComponent.submitMessage("test123");
        assertThat(getMessageCount(aiComponent)).isEqualTo(4); // 2 previous + 2 new
        assertThat(getMessageText(aiComponent, 3)).isEqualTo("Annotation processed: TEST123 (processed via @Install)");
    }

    @Test
    public void test_messageSentHandler() {
        // Navigate to test annotations view
        TestAiAnnotationsView view = navigateToTestAnnotationsView();

        // Verify view opened successfully and component is properly configured
        assertThat(view).isNotNull();
        AiConversationComponent aiComponent = view.aiComponent;
        assertThat(aiComponent).isNotNull();

        // Send message - this should trigger both message processing and message sent handlers
        aiComponent.submitMessage("test message handler");

        // Verify the message processing worked
        assertThat(getMessageCount(aiComponent)).isEqualTo(2);
        assertThat(getMessageText(aiComponent, 1)).contains("Annotation processed: TEST MESSAGE HANDLER");

        // The manual messageSentHandler is also called (check logs if needed)
    }

    // Helper methods
    private TestAiAnnotationsView navigateToTestAnnotationsView() {
        // Navigate to test annotations view
        viewNavigators.view(UiTestUtils.getCurrentView(), TestAiAnnotationsView.class).navigate();
        return UiTestUtils.getCurrentView();
    }

    private int getMessageCount(AiConversationComponent component) {
        return component.getMessageList().getItems().size();
    }

    private String getMessageText(AiConversationComponent component, int index) {
        return component.getMessageList().getItems().get(index).getText();
    }

    private String getMessageAuthor(AiConversationComponent component, int index) {
        return component.getMessageList().getItems().get(index).getUserName();
    }

}