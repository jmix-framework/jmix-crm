package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
class PendingAssistantResponseSupport {

    private final DataManager dataManager;

    PendingAssistantResponseSupport(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    Optional<ChatMessage> findPendingUserMessage(AiConversation conversation) {
        if (conversation == null) {
            return Optional.empty();
        }

        ChatMessage lastMessage = loadLatestPersistedMessage(conversation)
                .or(() -> latestLoadedMessage(conversation))
                .orElse(null);

        return lastMessage != null && ChatMessageType.USER.equals(lastMessage.getType())
                ? Optional.of(lastMessage)
                : Optional.empty();
    }

    private Optional<ChatMessage> loadLatestPersistedMessage(AiConversation conversation) {
        if (conversation.getId() == null) {
            return Optional.empty();
        }

        return dataManager.load(ChatMessage.class)
                .query("e.conversation.id = :conversationId order by e.createdDate desc, e.id desc")
                .parameter("conversationId", conversation.getId())
                .maxResults(1)
                .optional();
    }

    private Optional<ChatMessage> latestLoadedMessage(AiConversation conversation) {
        List<ChatMessage> messages = conversation.getMessages();
        if (messages == null || messages.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(messages.stream()
                .filter(message -> message.getCreatedDate() != null)
                .max(Comparator.comparing(ChatMessage::getCreatedDate))
                .orElse(messages.get(messages.size() - 1)));
    }
}
