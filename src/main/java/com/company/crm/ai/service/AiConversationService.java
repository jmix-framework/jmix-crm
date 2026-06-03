package com.company.crm.ai.service;

import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.company.crm.app.util.common.StreamUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Service for AI conversation lifecycle operations.
 */
@Service
public class AiConversationService {

    private final DataManager dataManager;
    private final Messages messages;

    public AiConversationService(DataManager dataManager,
                                 Messages messages) {
        this.dataManager = dataManager;
        this.messages = messages;
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

    public StartedConversation startConversation(String prompt,
                                                 List<String> entityReferences,
                                                 List<PendingAttachmentInput> attachments) {
        String trimmedPrompt = requirePrompt(prompt);

        AiConversation conversation = createNewConversation();
        ChatMessage firstUserMessage = createUserMessageAndEnsureStarted(
                conversation,
                trimmedPrompt,
                entityReferences,
                attachments
        );

        return new StartedConversation(conversation, firstUserMessage);
    }

    public ChatMessage createUserMessageAndEnsureStarted(AiConversation conversation,
                                                         String prompt,
                                                         List<String> entityReferences,
                                                         List<PendingAttachmentInput> attachments) {
        String trimmedPrompt = requirePrompt(prompt);

        ensureFirstMessageSent(conversation);

        return createUserMessage(
                conversation,
                trimmedPrompt,
                entityReferences,
                attachments
        );
    }

    public ChatMessage createUserMessage(AiConversation conversation,
                                         String text,
                                         List<String> entityReferences,
                                         List<PendingAttachmentInput> attachments) {
        String trimmedText = requirePrompt(text);

        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(conversation);
        message.setType(ChatMessageType.USER);
        message.setContent(trimmedText);

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
        List<String> distinctReferences = distinctEntityReferences(entityReferences);

        return IntStream.range(0, distinctReferences.size())
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
        return StreamUtils.safeStream(attachments)
                .filter(Objects::nonNull)
                .filter(attachment -> attachment.fileRef() != null)
                .map(attachment -> createAttachment(message, attachment))
                .toList();
    }

    private AiConversationAttachment createAttachment(ChatMessage message, PendingAttachmentInput pendingAttachment) {
        String fileName = pendingAttachment.resolvedFileName();

        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setMessage(message);
        attachment.setFile(pendingAttachment.fileRef());
        attachment.setFileName(fileName);
        attachment.setTitle(fileName);
        attachment.setOrigin(AiAttachmentOrigin.USER_UPLOADED);
        return attachment;
    }

    private List<String> distinctEntityReferences(List<String> entityReferences) {
        return StreamUtils.safeStream(entityReferences)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
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
}
