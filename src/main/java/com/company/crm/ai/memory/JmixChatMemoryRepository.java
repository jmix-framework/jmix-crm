package com.company.crm.ai.memory;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
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
    private static final String CRM_MESSAGE_TYPE_METADATA_KEY = "crmMessageType";
    private static final String ATTACHMENT_METADATA_VALUE = "ATTACHMENT";
    private static final String USER_UPLOAD_METADATA_VALUE = "USER_UPLOAD";

    private final DataManager dataManager;

    public JmixChatMemoryRepository(DataManager dataManager) {
        this.dataManager = dataManager;
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

        markNewMessagesForSavingInSaveContext(messages, existingMessages, conversation, saveContext);

        saveContext.setDiscardSaved(true);
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

                    saveContext.setDiscardSaved(true);
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
        return mapTypeToMessage(content, type, chatMessage.getId());
    }

    private ChatMessageType mapMessageToType(Message message) {
        return switch (message) {
            case UserMessage userMessage when isAttachmentMessage(userMessage) -> ChatMessageType.ATTACHMENT;
            case UserMessage userMessage when isUserUploadMessage(userMessage) -> ChatMessageType.USER_UPLOAD;
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

        final Map<String, Object> metadata = switch (type) {
            case ATTACHMENT -> Map.of(
                    ENTITY_ID_METADATA_KEY, entityId,
                    CRM_MESSAGE_TYPE_METADATA_KEY, ATTACHMENT_METADATA_VALUE
            );
            case USER_UPLOAD -> Map.of(
                    ENTITY_ID_METADATA_KEY, entityId,
                    CRM_MESSAGE_TYPE_METADATA_KEY, USER_UPLOAD_METADATA_VALUE
            );
            default -> Map.of(ENTITY_ID_METADATA_KEY, entityId);
        };
        return switch (type) {
            case USER -> UserMessage.builder().text(content)
                    .metadata(metadata).build();
            case ATTACHMENT -> UserMessage.builder().text(content)
                    .metadata(metadata).build();
            case USER_UPLOAD -> UserMessage.builder().text(content)
                    .metadata(metadata).build();
            case ASSISTANT, TOOL -> AssistantMessage.builder()
                    .content(content)
                    .properties(metadata).build();
            case SYSTEM -> SystemMessage.builder().text(content).metadata(metadata).build();
            case null -> SystemMessage.builder().text(content).build();
        };
    }

    private boolean isUserUploadMessage(UserMessage userMessage) {
        Object rawMessageType = userMessage.getMetadata().get(CRM_MESSAGE_TYPE_METADATA_KEY);
        return USER_UPLOAD_METADATA_VALUE.equals(rawMessageType);
    }

    private boolean isAttachmentMessage(UserMessage userMessage) {
        Object rawMessageType = userMessage.getMetadata().get(CRM_MESSAGE_TYPE_METADATA_KEY);
        return ATTACHMENT_METADATA_VALUE.equals(rawMessageType);
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
                .sort(Sort.by(Sort.Order.asc("createdDate"), Sort.Order.asc("id")))
                .list();
    }

}
