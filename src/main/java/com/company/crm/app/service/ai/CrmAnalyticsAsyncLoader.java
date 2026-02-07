package com.company.crm.app.service.ai;

import com.company.crm.view.component.aiconversation.AiConversationComponent;
import com.company.crm.view.component.aiconversation.AiConversationComponentAsyncMessageProcessor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import com.vaadin.flow.component.messages.MessageListItem;

import java.util.List;

/**
 * CRM-specific async loader that handles AI analytics processing.
 * Uses the generic AiComponentAsyncConversationLoader for UI updates.
 */
@Component
public class CrmAnalyticsAsyncLoader {

    private final CrmAnalyticsService crmAnalyticsService;
    private final AiConversationComponentAsyncMessageProcessor aiConversationComponentAsyncMessageProcessor;
    private final ChatMemoryRepository chatMemoryRepository;

    public CrmAnalyticsAsyncLoader(CrmAnalyticsService crmAnalyticsService,
                                 AiConversationComponentAsyncMessageProcessor aiConversationComponentAsyncMessageProcessor,
                                 ChatMemoryRepository chatMemoryRepository) {
        this.crmAnalyticsService = crmAnalyticsService;
        this.aiConversationComponentAsyncMessageProcessor = aiConversationComponentAsyncMessageProcessor;
        this.chatMemoryRepository = chatMemoryRepository;
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

    /**
     * Loads conversation history from the chat memory repository.
     *
     * @param conversationId the conversation ID
     * @return list of message list items for the UI
     */
    public List<MessageListItem> loadConversationHistory(String conversationId) {
        List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

        return messages.stream()
                .map(this::createMessageItem)
                .toList();
    }

    private MessageListItem createMessageItem(Message message) {
        boolean isAssistant = message instanceof AssistantMessage;
        MessageListItem item = new MessageListItem(
                message.getText(),
                isAssistant ? "Assistant" : "User"
        );
        item.setUserColorIndex(isAssistant ? 2 : 1);
        return item;
    }
}