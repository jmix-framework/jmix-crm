package com.company.crm.ai.jmix.memory;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.TimeSource;
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

import java.util.*;
import java.util.stream.Collectors;

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


    private static final Logger log = LoggerFactory.getLogger(JmixChatMemoryRepository.class);
    private static final String ENTITY_ID_METADATA_KEY = "jmixEntityId";

    private final DataManager dataManager;
    private final TimeSource timeSource;

    public JmixChatMemoryRepository(DataManager dataManager, TimeSource timeSource) {
        this.dataManager = dataManager;
        this.timeSource = timeSource;
    }

    @Override
    public List<String> findConversationIds() {
        try {
            return dataManager.loadValue("select c.id from AiConversation c", UUID.class)
                    .list()
                    .stream()
                    .map(UUID::toString)
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

        markRemovedMessageForRemovalInSaveContext(messages, existingMessages, saveContext);

        markNewMessagesForSavingInSaveContext(messages, existingMessages, conversation, saveContext);

        dataManager.save(saveContext);
    }

    private void markNewMessagesForSavingInSaveContext(List<Message> messages, List<ChatMessage> existingMessages, AiConversation conversation, SaveContext saveContext) {
        Set<UUID> existingEntityIds = existingMessages.stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toSet());

        messages.stream()
                .filter(message -> {
                    UUID entityId = (UUID) message.getMetadata().get(ENTITY_ID_METADATA_KEY);
                    return entityId == null || !existingEntityIds.contains(entityId);
                })
                .map(newMessage -> mapMessageToEntity(newMessage, conversation))
                .forEach(saveContext::saving);
    }

    private void markRemovedMessageForRemovalInSaveContext(List<Message> messages, List<ChatMessage> existingMessages, SaveContext saveContext) {
        Set<UUID> newMessageEntityIds = messages.stream()
                .map(message -> (UUID) message.getMetadata().get(ENTITY_ID_METADATA_KEY))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        existingMessages.stream()
                .filter(existingMessage -> !newMessageEntityIds.contains(existingMessage.getId()))
                .forEach(saveContext::removing);
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
        chatMessage.setCreatedDate(timeSource.now().toOffsetDateTime());
        return chatMessage;
    }

    private Message mapEntityToMessage(ChatMessage chatMessage) {
        String content = chatMessage.getContent();
        ChatMessageType type = chatMessage.getType();
        return mapTypeToMessage(content, type, chatMessage.getId());
    }

    private ChatMessageType mapMessageToType(Message message) {
        return switch (message) {
            case UserMessage ignored -> ChatMessageType.USER;
            case AssistantMessage ignored -> ChatMessageType.ASSISTANT;
            case SystemMessage ignored -> ChatMessageType.SYSTEM;
            case null, default -> ChatMessageType.TOOL;
        };
    }

    private Message mapTypeToMessage(String content, ChatMessageType type, UUID entityId) {
        if (type == null) {
            return new SystemMessage(content != null ? content : "");
        }

        final Map<String, Object> metadata = Map.of(ENTITY_ID_METADATA_KEY, entityId);
        return switch (type) {
            case USER -> UserMessage.builder().text(content)
                    .metadata(metadata).build();
            case ASSISTANT, TOOL -> AssistantMessage.builder()
                    .content(content)
                    .properties(metadata).build();
            case SYSTEM -> SystemMessage.builder().text(content).metadata(metadata).build();
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

}
