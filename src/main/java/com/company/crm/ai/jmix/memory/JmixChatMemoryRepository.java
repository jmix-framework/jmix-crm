package com.company.crm.ai.jmix.memory;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.querycondition.PropertyCondition;
import io.jmix.core.Sort;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Jmix-based implementation of Spring AI ChatMemoryRepository.
 *
 * <p>This repository provides persistent storage for AI chat conversations using Jmix data management capabilities.
 * It stores chat messages and conversation history in the database through Jmix entities, enabling:
 * <ul>
 *   <li>Persistent conversation memory across application restarts</li>
 *   <li>Multi-user conversation support with proper isolation</li>
 *   <li>Full integration with Jmix security and data access patterns</li>
 *   <li>Transactional consistency for chat operations</li>
 * </ul>
 *
 * <p>The implementation handles conversion between Spring AI message types (UserMessage, AssistantMessage, SystemMessage)
 * and Jmix ChatMessage entities, maintaining message ordering and conversation context.
 *
 * @see ChatMemoryRepository
 * @see AiConversation
 * @see ChatMessage
 */
@Component
public class JmixChatMemoryRepository implements ChatMemoryRepository {

    private final DataManager dataManager;
    private static final Logger log = LoggerFactory.getLogger(JmixChatMemoryRepository.class);

    public JmixChatMemoryRepository(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public List<String> findConversationIds() {
        try {
            return dataManager.loadValues("select c.id from AiConversation c")
                    .properties("id")
                    .list().stream()
                    .map(keyValueEntity -> keyValueEntity.getValue("id").toString())
                    .toList();
        } catch (Exception e) {
            log.error("Error finding conversation IDs", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Message> findByConversationId(@NonNull String conversationId) {

        UUID uuid = parseConversationId(conversationId);
        return dataManager.load(AiConversation.class)
                .id(uuid)
                .optional()
                .map(conversation -> loadChatMessages(uuid).stream()
                        .map(this::mapEntityToMessage)
                        .toList())
                .orElse(Collections.emptyList());

    }

    @Override
    @Transactional
    public void saveAll(@NonNull String conversationId, List<Message> messages) {

        UUID uuid = parseConversationId(conversationId);
        AiConversation conversation = findOrCreateConversation(uuid);

        SaveContext saveContext = new SaveContext();

        List<ChatMessage> existingMessages = loadChatMessages(uuid);
        if (!messagesAreEqual(messages, existingMessages)) {
            existingMessages.forEach(saveContext::removing);

            OffsetDateTime baseTime = OffsetDateTime.now();
            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                ChatMessage chatMessage = mapMessageToEntity(message, conversation);
                // Use larger time increments to ensure proper ordering even across different saveAll calls
                chatMessage.setCreatedDate(baseTime.plusSeconds(i));
                saveContext.saving(chatMessage);
            }
        }

        dataManager.save(saveContext);
    }

    @Override
    @Transactional
    public void deleteByConversationId(@NonNull String conversationId) {

        UUID uuid = parseConversationId(conversationId);
        dataManager.load(AiConversation.class)
                .id(uuid)
                .optional()
                .ifPresent(conversation -> {
                    SaveContext saveContext = new SaveContext();
                    loadChatMessages(uuid).forEach(saveContext::removing);
                    saveContext.removing(conversation);

                    dataManager.save(saveContext);
                });

    }

    private AiConversation findOrCreateConversation(UUID conversationId) {
        return dataManager.load(AiConversation.class)
                .id(conversationId)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
    }

    private ChatMessage mapMessageToEntity(Message message, AiConversation conversation) {
        ChatMessage chatMessage = dataManager.create(ChatMessage.class);
        chatMessage.setConversation(conversation);
        chatMessage.setContent(message.getText());
        chatMessage.setType(mapMessageToType(message));
        return chatMessage;
    }

    private Message mapEntityToMessage(ChatMessage chatMessage) {
        String content = chatMessage.getContent();
        ChatMessageType type = chatMessage.getType();
        return mapTypeToMessage(content, type);
    }

    private ChatMessageType mapMessageToType(Message message) {
        return switch (message) {
            case UserMessage ignored -> ChatMessageType.USER;
            case AssistantMessage ignored -> ChatMessageType.ASSISTANT;
            case SystemMessage ignored -> ChatMessageType.SYSTEM;
            case null, default -> ChatMessageType.TOOL;
        };
    }

    private Message mapTypeToMessage(String content, ChatMessageType type) {
        if (type == null) {
            return new SystemMessage(content != null ? content : "");
        }

        return switch (type) {
            case USER -> new UserMessage(content != null ? content : "");
            case ASSISTANT -> new AssistantMessage(content != null ? content : "");
            case SYSTEM -> new SystemMessage(content != null ? content : "");
            case TOOL -> new AssistantMessage(content != null ? content : ""); // Fallback to assistant
        };
    }

    private UUID parseConversationId(String conversationId) {
        try {
            return UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for conversation ID: {}", conversationId, e);
            throw new IllegalArgumentException("Invalid conversation ID format: " + conversationId, e);
        }
    }

    private List<ChatMessage> loadChatMessages(UUID conversationId) {
        return dataManager.load(ChatMessage.class)
                .condition(PropertyCondition.equal("conversation.id", conversationId))
                .sort(Sort.by("createdDate"))
                .list();
    }

    private boolean messagesAreEqual(List<Message> newMessages, List<ChatMessage> existingMessages) {
        if (newMessages.size() != existingMessages.size()) {
            return false;
        }

        for (int i = 0; i < newMessages.size(); i++) {
            Message newMessage = newMessages.get(i);
            ChatMessage existingMessage = existingMessages.get(i);

            if (!newMessage.getText().equals(existingMessage.getContent()) ||
                    !mapMessageToType(newMessage).equals(existingMessage.getType())) {
                return false;
            }
        }
        return true;
    }
}
