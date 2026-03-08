package com.company.crm.ai.service;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import org.springframework.stereotype.Service;

/**
 * Service for AI conversation management including creation and title generation.
 */
@Service
public class AiConversationService {

    private final DataManager dataManager;
    private final Messages messages;

    public AiConversationService(DataManager dataManager, Messages messages) {
        this.dataManager = dataManager;
        this.messages = messages;
    }

    /**
     * Creates a new AI conversation with welcome message.
     *
     * @param welcomeMessage the welcome message text
     * @return the created conversation
     */
    public AiConversation createNewConversation(String welcomeMessage) {
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle(messages.formatMessage(AiConversation.class, "defaultTitle"));
        conversation.setFirstMessageSent(false);

        ChatMessage welcomeMessageEntity = dataManager.create(ChatMessage.class);
        welcomeMessageEntity.setConversation(conversation);
        welcomeMessageEntity.setContent(welcomeMessage);
        welcomeMessageEntity.setType(ChatMessageType.ASSISTANT);

        SaveContext saveContext = new SaveContext();
        saveContext.saving(conversation);
        saveContext.saving(welcomeMessageEntity);
        return dataManager.save(saveContext).get(conversation);
    }
}
