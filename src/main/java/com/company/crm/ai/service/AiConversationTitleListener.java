package com.company.crm.ai.service;

import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.event.EntityChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class AiConversationTitleListener {

    private static final Logger log = LoggerFactory.getLogger(AiConversationTitleListener.class);

    private final AiConversationTitleService titleService;
    private final DataManager dataManager;

    public AiConversationTitleListener(AiConversationTitleService titleService, DataManager dataManager) {
        this.titleService = titleService;
        this.dataManager = dataManager;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onChatMessageCreated(EntityChangedEvent<ChatMessage> event) {
        if (event.getType() != EntityChangedEvent.Type.CREATED) {
            return;
        }

        try {
            ChatMessage message = dataManager.load(event.getEntityId()).one();
            if (message.getType() != ChatMessageType.USER) {
                return;
            }

            UUID conversationId = message.getConversation().getId();
            titleService.generateTitleIfNeeded(conversationId);
        } catch (Exception e) {
            log.warn("Error triggering title generation", e);
        }
    }
}
