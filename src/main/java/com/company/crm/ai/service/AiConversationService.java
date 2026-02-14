package com.company.crm.ai.service;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.TimeSource;
import io.jmix.core.querycondition.PropertyCondition;
import io.jmix.core.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for AI conversation management including creation and title generation.
 */
@Service
public class AiConversationService {

    private final DataManager dataManager;
    private final TimeSource timeSource;

    public AiConversationService(DataManager dataManager, TimeSource timeSource) {
        this.dataManager = dataManager;
        this.timeSource = timeSource;
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
        welcomeMessageEntity.setCreatedDate(timeSource.now().toOffsetDateTime());

        SaveContext saveContext = new SaveContext();
        saveContext.saving(conversation);
        saveContext.saving(welcomeMessageEntity);
        return dataManager.save(saveContext).get(conversation);
    }
}