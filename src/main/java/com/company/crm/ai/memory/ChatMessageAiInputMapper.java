package com.company.crm.ai.memory;

import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiAttachmentMediaResolver;
import com.company.crm.ai.service.AiAttachmentMediaResolver.ResolvedAttachmentInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatMessageAiInputMapper {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageAiInputMapper.class);

    public static final String ENTITY_ID_METADATA_KEY = "jmixEntityId";

    private final EntityReferenceContentResolver entityReferenceContentResolver;
    private final AiAttachmentMediaResolver attachmentMediaResolver;

    public ChatMessageAiInputMapper(EntityReferenceContentResolver entityReferenceContentResolver,
                                    AiAttachmentMediaResolver attachmentMediaResolver) {
        this.entityReferenceContentResolver = entityReferenceContentResolver;
        this.attachmentMediaResolver = attachmentMediaResolver;
    }

    public Message map(ChatMessage chatMessage) {
        String content = chatMessage.getContent();
        ChatMessageType type = chatMessage.getType();
        List<Media> media = List.of();

        log.debug("Mapping chat message to AI input: {} (type: {})", chatMessage.getId(), type);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ENTITY_ID_METADATA_KEY, chatMessage.getId());

        content = appendContentBlock(content,
                entityReferenceContentResolver.resolveContext(chatMessage.getEntityReferences()));

        for (AiConversationAttachment attachment : safeAttachments(chatMessage)) {
            try {
                ResolvedAttachmentInput resolvedAttachment = attachmentMediaResolver.resolve(attachment, null);
                if (!resolvedAttachment.media().isEmpty()) {
                    media = appendMedia(media, resolvedAttachment.media());
                }
                content = appendContentBlock(content, resolvedAttachment.textContext());
            } catch (Exception e) {
                log.warn("Failed to load attachment media {}: {}", attachment.getId(), e.getMessage());
            }
        }

        return mapTypeToMessage(content, type, metadata, media);
    }

    private Message mapTypeToMessage(String content, ChatMessageType type, Map<String, Object> metadata, List<Media> media) {
        if (type == null) {
            return new SystemMessage(content != null ? content : "");
        }

        return switch (type) {
            case USER -> UserMessage.builder()
                    .text(content)
                    .media(media)
                    .metadata(metadata)
                    .build();
            case ASSISTANT, TOOL -> AssistantMessage.builder().content(content).properties(metadata).build();
            case SYSTEM -> SystemMessage.builder().text(content).metadata(metadata).build();
        };
    }

    private List<AiConversationAttachment> safeAttachments(ChatMessage chatMessage) {
        return chatMessage.getAttachments() != null ? chatMessage.getAttachments() : List.of();
    }

    private List<Media> appendMedia(List<Media> existing, List<Media> additional) {
        if (existing.isEmpty()) {
            return additional;
        }
        List<Media> merged = new ArrayList<>(existing);
        merged.addAll(additional);
        return merged;
    }

    private String appendContentBlock(String content, String block) {
        if (block == null || block.isBlank()) {
            return content;
        }
        return (content != null && !content.isBlank() ? content + "\n\n" : "") + block;
    }
}
