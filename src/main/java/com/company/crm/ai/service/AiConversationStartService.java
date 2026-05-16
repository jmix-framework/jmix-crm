package com.company.crm.ai.service;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import io.jmix.core.DataManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiConversationStartService {

    private final AiConversationService aiConversationService;
    private final DataManager dataManager;

    public AiConversationStartService(AiConversationService aiConversationService,
                                      DataManager dataManager) {
        this.aiConversationService = aiConversationService;
        this.dataManager = dataManager;
    }

    @Transactional
    public StartedConversation startConversation(String prompt,
                                                 List<String> entityReferences,
                                                 List<PendingAttachmentInput> attachments) {
        String trimmedPrompt = requirePrompt(prompt);

        AiConversation conversation = aiConversationService.createNewConversation();
        ChatMessage userMessage = createUserMessageAndEnsureStarted(
                conversation,
                trimmedPrompt,
                entityReferences,
                attachments
        );

        return new StartedConversation(conversation, userMessage);
    }

    @Transactional
    public ChatMessage createUserMessageAndEnsureStarted(AiConversation conversation,
                                                         String prompt,
                                                         List<String> entityReferences,
                                                         List<PendingAttachmentInput> attachments) {
        if (conversation == null || conversation.getId() == null) {
            throw new IllegalArgumentException("Conversation is required to start a chat turn.");
        }

        String trimmedPrompt = requirePrompt(prompt);
        ensureFirstMessageSent(conversation);

        return aiConversationService.createUserMessage(
                conversation,
                trimmedPrompt,
                entityReferences,
                attachments
        );
    }

    private void ensureFirstMessageSent(AiConversation conversation) {
        if (Boolean.TRUE.equals(conversation.getFirstMessageSent())) {
            return;
        }

        conversation.setFirstMessageSent(true);
        dataManager.save(conversation);
    }

    private String requirePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("A human message text is required.");
        }
        return prompt.trim();
    }

    public record StartedConversation(AiConversation conversation,
                                      ChatMessage firstUserMessage) {
    }
}
