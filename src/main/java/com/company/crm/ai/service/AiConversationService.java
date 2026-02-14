package com.company.crm.ai.service;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import org.springframework.stereotype.Service;

/**
 * Service for AI conversation management including creation and title generation.
 */
@Service
public class AiConversationService {

    private final DataManager dataManager;

    public AiConversationService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Creates a new AI conversation with welcome message.
     *
     * @param welcomeMessage the welcome message text
     * @return the created conversation
     */
    public AiConversation createNewConversation(String welcomeMessage) {
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle("AI Chat Session");

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