package com.company.crm.ai.view.context;

import com.company.crm.ai.model.AiConversationAttachment;

import java.util.List;

public record ConversationContextItems(
        List<String> entityReferences,
        List<AiConversationAttachment> generatedAttachments,
        List<AiConversationAttachment> uploadedAttachments
) {

    static ConversationContextItems empty() {
        return new ConversationContextItems(List.of(), List.of(), List.of());
    }

    int totalCount() {
        return entityReferences.size() + generatedAttachments.size() + uploadedAttachments.size();
    }

    boolean isEmpty() {
        return totalCount() == 0;
    }
}
