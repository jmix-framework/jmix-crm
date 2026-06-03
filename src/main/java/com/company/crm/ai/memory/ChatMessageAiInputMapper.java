package com.company.crm.ai.memory;

import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.prompt.AiPromptContentBuilder;
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
import com.company.crm.app.util.common.StreamUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        ChatMessageType type = chatMessage.getType();

        log.debug("Mapping chat message to AI input: {} (type: {})", chatMessage.getId(), type);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ENTITY_ID_METADATA_KEY, chatMessage.getId());

        List<ResolvedAttachmentInput> attachments = resolveAttachments(chatMessage);
        String content = buildContent(chatMessage, attachments);
        List<Media> media = attachments.stream()
                .flatMap(attachment -> attachment.media().stream())
                .toList();

        return mapTypeToMessage(content, type, metadata, media);
    }

    private Message mapTypeToMessage(String content, ChatMessageType type, Map<String, Object> metadata, List<Media> media) {
        String messageContent = content != null ? content : "";

        if (type == null) {
            return new SystemMessage(messageContent);
        }

        return switch (type) {
            case USER -> UserMessage.builder()
                    .text(messageContent)
                    .media(media)
                    .metadata(metadata)
                    .build();
            case ASSISTANT, TOOL -> AssistantMessage.builder().content(messageContent).properties(metadata).build();
            case SYSTEM -> SystemMessage.builder().text(messageContent).metadata(metadata).build();
        };
    }

    private String buildContent(ChatMessage chatMessage, List<ResolvedAttachmentInput> attachments) {
        return AiPromptContentBuilder.create()
                .appendParagraph(chatMessage.getContent())
                .appendParagraph(entityReferenceContentResolver.resolveContext(chatMessage.getEntityReferences()))
                .appendParagraphs(attachments.stream().map(ResolvedAttachmentInput::textContext))
                .build();
    }

    private List<ResolvedAttachmentInput> resolveAttachments(ChatMessage chatMessage) {
        return StreamUtils.safeStream(chatMessage.getAttachments())
                .map(this::resolveAttachment)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ResolvedAttachmentInput> resolveAttachment(AiConversationAttachment attachment) {
        try {
            return Optional.of(attachmentMediaResolver.resolve(attachment, null));
        } catch (Exception e) {
            log.warn("Failed to load attachment media {}: {}", attachment.getId(), e.getMessage());
            return Optional.empty();
        }
    }

}
