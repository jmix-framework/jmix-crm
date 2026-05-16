package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

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

        LinkedHashSet<String> entityReferences = new LinkedHashSet<>();
        List<AiConversationAttachment> generated = new ArrayList<>();
        List<AiConversationAttachment> uploaded = new ArrayList<>();

        for (ChatMessage message : messages) {
            Optional.ofNullable(message.getEntityReferences()).orElse(List.of()).stream()
                    .map(ChatMessageEntityReference::getEntityReference)
                    .filter(ref -> ref != null && !ref.isBlank())
                    .forEach(entityReferences::add);

            Optional.ofNullable(message.getAttachments()).orElse(List.of()).forEach(attachment -> {
                if (AiAttachmentType.AI_GENERATED.equals(attachment.getType())) {
                    generated.add(attachment);
                } else {
                    uploaded.add(attachment);
                }
            });
        }

        return new ConversationContextItems(
                List.copyOf(entityReferences),
                List.copyOf(generated),
                List.copyOf(uploaded)
        );
    }
}
