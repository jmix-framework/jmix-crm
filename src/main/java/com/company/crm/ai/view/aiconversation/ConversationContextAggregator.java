package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class ConversationContextAggregator {

    ConversationContextItems aggregate(AiConversation conversation) {
        if (conversation == null) {
            return ConversationContextItems.empty();
        }

        List<ChatMessage> messages = Optional.ofNullable(conversation.getMessages())
                .orElse(List.of())
                .stream()
                .sorted(Comparator
                        .comparing(ChatMessage::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ChatMessage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<String> entityReferences = messages.stream()
                .flatMap(message -> Optional.ofNullable(message.getEntityReferences()).orElse(List.of()).stream())
                .map(ChatMessageEntityReference::getEntityReference)
                .filter(ref -> ref != null && !ref.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));

        List<AiConversationAttachment> attachments = messages.stream()
                .flatMap(message -> Optional.ofNullable(message.getAttachments()).orElse(List.of()).stream())
                .toList();

        List<AiConversationAttachment> generated = attachments.stream()
                .filter(attachment -> AiAttachmentType.AI_GENERATED.equals(attachment.getType()))
                .toList();

        List<AiConversationAttachment> uploaded = attachments.stream()
                .filter(attachment -> !AiAttachmentType.AI_GENERATED.equals(attachment.getType()))
                .toList();

        return new ConversationContextItems(
                entityReferences,
                generated,
                uploaded
        );
    }
}
