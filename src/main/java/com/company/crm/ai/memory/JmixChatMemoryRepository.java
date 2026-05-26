package com.company.crm.ai.memory;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.core.Sort;
import io.jmix.core.querycondition.PropertyCondition;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Jmix-based implementation of Spring AI ChatMemoryRepository.
 *
 * <p>This repository provides persistent storage for AI chat conversations using Jmix data management capabilities.
 * It stores chat messages and conversation history in the database through Jmix entities.
 *
 * @see ChatMemoryRepository
 * @see AiConversation
 * @see ChatMessage
 */
@Component
public class JmixChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(JmixChatMemoryRepository.class);

    private final DataManager dataManager;
    private final ChatMessageAiInputMapper chatMessageAiInputMapper;

    public JmixChatMemoryRepository(DataManager dataManager,
                                    ChatMessageAiInputMapper chatMessageAiInputMapper) {
        this.dataManager = dataManager;
        this.chatMessageAiInputMapper = chatMessageAiInputMapper;
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
        UUID uuid = conversationUuid(conversationId);
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
        UUID uuid = conversationUuid(conversationId);
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
                    UUID entityId = (UUID) message.getMetadata().get(ChatMessageAiInputMapper.ENTITY_ID_METADATA_KEY);
                    return entityId == null || !existingEntityIds.contains(entityId);
                })
                .map(newMessage -> mapMessageToEntity(newMessage, conversation))
                .forEach(saveContext::saving);
    }

    @Override
    @Transactional
    public void deleteByConversationId(@NonNull String conversationId) {
        UUID uuid = conversationUuid(conversationId);
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
        return chatMessageAiInputMapper.map(chatMessage);
    }

    private ChatMessageType mapMessageToType(Message message) {
        return switch (message) {
            case UserMessage ignored -> ChatMessageType.USER;
            case AssistantMessage ignored -> ChatMessageType.ASSISTANT;
            case SystemMessage ignored -> ChatMessageType.SYSTEM;
            case null, default -> ChatMessageType.TOOL;
        };
    }

    private UUID conversationUuid(String conversationId) {
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
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                        .add("entityReferences", FetchPlan.BASE)
                        .add("attachments", FetchPlan.BASE))
                .sort(Sort.by(Sort.Order.asc("createdDate"), Sort.Order.asc("id")))
                .list();
    }

}
