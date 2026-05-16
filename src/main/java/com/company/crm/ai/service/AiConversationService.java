package com.company.crm.ai.service;

import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Service for AI conversation lifecycle operations.
 */
@Service
public class AiConversationService {

    private final DataManager dataManager;
    private final Messages messages;
    private final TimeSource timeSource;
    private final CurrentAuthentication currentAuthentication;

    public AiConversationService(DataManager dataManager,
                                 Messages messages,
                                 TimeSource timeSource,
                                 CurrentAuthentication currentAuthentication) {
        this.dataManager = dataManager;
        this.messages = messages;
        this.timeSource = timeSource;
        this.currentAuthentication = currentAuthentication;
    }

    /**
     * Creates a new empty AI conversation.
     *
     * @return the created conversation
     */
    public AiConversation createNewConversation() {
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle(messages.formatMessage(AiConversation.class, "defaultTitle"));
        conversation.setFirstMessageSent(false);
        return dataManager.save(conversation);
    }

    public ChatMessage createUserMessage(AiConversation conversation,
                                         String text,
                                         List<String> entityReferences,
                                         List<PendingAttachmentInput> attachments) {
        if (conversation == null || conversation.getId() == null) {
            throw new IllegalArgumentException("Conversation is required to create a chat message.");
        }
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("A human message text is required.");
        }

        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(conversation);
        message.setType(ChatMessageType.USER);
        message.setContent(text.trim());
        message.setCreatedDate(now());
        message.setCreatedBy(currentAuthentication.getUser().getUsername());

        List<ChatMessageEntityReference> referenceEntities = createEntityReferences(message, entityReferences);
        List<AiConversationAttachment> attachmentEntities = createAttachments(message, attachments);

        message.setEntityReferences(referenceEntities);
        message.setAttachments(attachmentEntities);

        SaveContext saveContext = new SaveContext();
        saveContext.saving(message);
        referenceEntities.forEach(saveContext::saving);
        attachmentEntities.forEach(saveContext::saving);

        return dataManager.save(saveContext).get(message);
    }

    private List<ChatMessageEntityReference> createEntityReferences(ChatMessage message, List<String> entityReferences) {
        List<String> distinctReferences = new LinkedHashSet<>(
                entityReferences != null ? entityReferences : List.<String>of()
        ).stream()
                .filter(StringUtils::hasText)
                .toList();

        return java.util.stream.IntStream.range(0, distinctReferences.size())
                .mapToObj(i -> {
                    ChatMessageEntityReference reference = dataManager.create(ChatMessageEntityReference.class);
                    reference.setMessage(message);
                    reference.setEntityReference(distinctReferences.get(i));
                    reference.setSortOrder(i);
                    return reference;
                })
                .toList();
    }

    private List<AiConversationAttachment> createAttachments(ChatMessage message, List<PendingAttachmentInput> attachments) {
        return (attachments != null ? attachments : List.<PendingAttachmentInput>of()).stream()
                .filter(Objects::nonNull)
                .filter(attachment -> attachment.fileRef() != null)
                .map(attachment -> createAttachment(message, attachment))
                .toList();
    }

    private AiConversationAttachment createAttachment(ChatMessage message, PendingAttachmentInput pendingAttachment) {
        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setMessage(message);
        attachment.setFile(pendingAttachment.fileRef());
        attachment.setFileName(resolveFileName(pendingAttachment));
        attachment.setTitle(resolveFileName(pendingAttachment));
        attachment.setType(AiAttachmentType.USER_UPLOADED);
        return attachment;
    }

    private String resolveFileName(PendingAttachmentInput attachment) {
        if (StringUtils.hasText(attachment.fileName())) {
            return attachment.fileName();
        }
        if (attachment.fileRef() != null && StringUtils.hasText(attachment.fileRef().getFileName())) {
            return attachment.fileRef().getFileName();
        }
        return "uploaded-file";
    }

    private OffsetDateTime now() {
        return timeSource.now().toOffsetDateTime();
    }
}
