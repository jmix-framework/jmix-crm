package com.company.crm.ai.view.context;

import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConversationContextAggregator {

    ConversationContextItems aggregate(AiConversation conversation) {
        if (conversation == null) {
            return ConversationContextItems.empty();
        }

        List<ChatMessage> messages = conversation.getSortedMessages();

        List<String> entityReferences = messages.stream()
                .flatMap(message -> Optional.ofNullable(message.getEntityReferences()).orElse(List.of()).stream())
                .map(ChatMessageEntityReference::getEntityReference)
                .filter(org.springframework.util.StringUtils::hasText)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));

        List<AiConversationAttachment> attachments = messages.stream()
                .flatMap(message -> Optional.ofNullable(message.getAttachments()).orElse(List.of()).stream())
                .toList();

        List<AiConversationAttachment> generated = attachments.stream()
                .filter(attachment -> AiAttachmentOrigin.AI_GENERATED.equals(attachment.getOrigin()))
                .toList();

        List<AiConversationAttachment> uploaded = attachments.stream()
                .filter(attachment -> !AiAttachmentOrigin.AI_GENERATED.equals(attachment.getOrigin()))
                .toList();

        return new ConversationContextItems(
                entityReferences,
                generated,
                uploaded
        );
    }
}
