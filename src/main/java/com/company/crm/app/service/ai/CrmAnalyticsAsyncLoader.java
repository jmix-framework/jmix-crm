package com.company.crm.app.service.ai;

import com.company.crm.view.component.aiconversation.AiConversationComponent;
import com.company.crm.view.component.aiconversation.AiConversationComponentAsyncMessageProcessor;
import org.springframework.stereotype.Component;

/**
 * CRM-specific async loader that handles AI analytics processing.
 * Uses the generic AiComponentAsyncConversationLoader for UI updates.
 */
@Component
public class CrmAnalyticsAsyncLoader {

    private final CrmAnalyticsService crmAnalyticsService;
    private final AiConversationComponentAsyncMessageProcessor aiConversationComponentAsyncMessageProcessor;

    public CrmAnalyticsAsyncLoader(CrmAnalyticsService crmAnalyticsService,
                                 AiConversationComponentAsyncMessageProcessor aiConversationComponentAsyncMessageProcessor) {
        this.crmAnalyticsService = crmAnalyticsService;
        this.aiConversationComponentAsyncMessageProcessor = aiConversationComponentAsyncMessageProcessor;
    }

    /**
     * Processes a user message asynchronously using CRM analytics service and updates the AI component.
     *
     * @param userMessage the user's message
     * @param conversationId the conversation ID
     * @param aiComponent the AI component to update
     */
    public void processMessageAsync(String userMessage, String conversationId,
                                  AiConversationComponent aiComponent) {
        aiConversationComponentAsyncMessageProcessor.processMessage(
            userMessage,
            conversationId,
            aiComponent,
            (message, convId) -> crmAnalyticsService.processBusinessQuestion(message, convId)
        );
    }
}