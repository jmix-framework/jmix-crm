package com.company.crm.app.service.ai;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import com.vaadin.flow.component.messages.MessageListItem;

import java.util.List;

/**
 * CRM-specific service that handles AI analytics processing and history loading.
 * Provides methods for both async processing and conversation history retrieval.
 */
@Component
public class CrmAnalyticsAsyncLoader {

    private final CrmAnalyticsService crmAnalyticsService;
    private final ChatMemoryRepository chatMemoryRepository;

    public CrmAnalyticsAsyncLoader(CrmAnalyticsService crmAnalyticsService,
                                 ChatMemoryRepository chatMemoryRepository) {
        this.crmAnalyticsService = crmAnalyticsService;
        this.chatMemoryRepository = chatMemoryRepository;
    }

    /**
     * Processes a business question using the CRM analytics service.
     * This method is synchronous and should be called within async context.
     *
     * @param userMessage the user's message
     * @param conversationId the conversation ID
     * @return the AI response
     */
    public String processBusinessQuestion(String userMessage, String conversationId) {
        return crmAnalyticsService.processBusinessQuestion(userMessage, conversationId);
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