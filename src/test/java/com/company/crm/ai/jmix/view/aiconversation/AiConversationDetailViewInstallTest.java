package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.util.extenstion.DataCleaner;
import com.company.crm.view.component.aiconversation.AiConversationComponent;
import io.jmix.flowui.testassist.UiTest;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test to verify that @Install pattern works correctly with AiConversationDetailView
 */
@UiTest
@ExtendWith(DataCleaner.class)
public class AiConversationDetailViewInstallTest extends AbstractUiTest {

    @MockBean
    private CrmAnalyticsService crmAnalyticsService;

    @Test
    public void test_installPattern_messageProcessorWorks() {
        // Mock the CRM analytics service
        when(crmAnalyticsService.processBusinessQuestion(anyString(), anyString()))
                .thenReturn("Mocked AI response from CRM Analytics Service");

        // Create test conversation
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle("Test Conversation");
        conversation = dataManager.save(conversation);

        // Navigate to detail view
        AiConversationDetailView view = navigateToDetailView(conversation.getId().toString());

        // Verify view opened successfully
        assertThat(view).isNotNull();
        assertThat(view.getEditedEntity()).isNotNull();
        assertThat(view.getEditedEntity().getId()).isEqualTo(conversation.getId());

        // Get the AI component
        AiConversationComponent aiComponent = view.aiComponent;
        assertThat(aiComponent).isNotNull();

        // Test that the @Install messageProcessor is working by sending a message
        aiComponent.submitMessage("Test message for @Install pattern");

        // Verify message was processed
        assertThat(getMessageCount(aiComponent)).isEqualTo(2); // user message + AI response

        // Verify user message
        String userMessage = getMessageText(aiComponent, 0);
        assertThat(userMessage).isEqualTo("Test message for @Install pattern");

        // Verify AI response from CRM analytics service
        String aiResponse = getMessageText(aiComponent, 1);
        assertThat(aiResponse).isEqualTo("Mocked AI response from CRM Analytics Service");
        assertThat(getMessageAuthor(aiComponent, 1)).isEqualTo("Assistant");
    }

    @Test
    public void test_installPattern_errorHandling() {
        // Mock the CRM analytics service to throw an exception
        when(crmAnalyticsService.processBusinessQuestion(anyString(), anyString()))
                .thenThrow(new RuntimeException("Test error"));

        // Create test conversation
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle("Error Test Conversation");
        conversation = dataManager.save(conversation);

        // Navigate to detail view
        AiConversationDetailView view = navigateToDetailView(conversation.getId().toString());

        // Get the AI component
        AiConversationComponent aiComponent = view.aiComponent;

        // Test error handling
        aiComponent.submitMessage("This will cause an error");

        // Verify error message was handled gracefully
        assertThat(getMessageCount(aiComponent)).isEqualTo(2); // user message + error response

        String errorResponse = getMessageText(aiComponent, 1);
        assertThat(errorResponse).contains("I'm sorry, I encountered an error while processing your request");
        assertThat(errorResponse).contains("Test error");
    }

    // Helper methods
    private AiConversationDetailView navigateToDetailView(String conversationId) {
        AiConversation conversation = dataManager.load(AiConversation.class).id(java.util.UUID.fromString(conversationId)).one();
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversation)
                .navigate();
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