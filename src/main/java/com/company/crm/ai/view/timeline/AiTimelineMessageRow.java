package com.company.crm.ai.view.timeline;

import com.company.crm.ai.view.component.card.AiConversationContextCardFactory;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;

import java.util.List;
import java.util.Optional;

public class AiTimelineMessageRow extends AbstractAiTimelineRow {

    public AiTimelineMessageRow() {
        super();
    }

    public void setMessage(ChatMessage message,
                           boolean assistant,
                           boolean isFresh,
                           String actorName,
                           String formattedTime,
                           AiConversationContextCardFactory contextCardFactory) {
        resetMessageClassNames();

        addClassNames("ai-timeline-message-row", assistant ? "ai-timeline-message-row-assistant" : "ai-timeline-message-row-user");
        if (assistant && isFresh) {
            addClassName("ai-timeline-message-row-fresh");
        }

        initRow(assistant, actorName, formattedTime);

        body.add(createMessageContent(message, assistant));

        List<AiConversationAttachment> attachments = message.getAttachments() != null ? message.getAttachments() : List.of();
        List<ChatMessageEntityReference> entityReferences = message.getEntityReferences() != null ? message.getEntityReferences() : List.of();
        if (!attachments.isEmpty() || !entityReferences.isEmpty()) {
            addClassName("ai-timeline-message-row-context");
            body.add(contextCardFactory.createMessageContextCardsGrid(entityReferences, attachments));
        }
    }

    private void resetMessageClassNames() {
        removeClassName("ai-timeline-message-row-fresh");
        removeClassName("ai-timeline-message-row-assistant");
        removeClassName("ai-timeline-message-row-user");
        removeClassName("ai-timeline-message-row-context");
    }

    private Component createMessageContent(ChatMessage message, boolean assistant) {
        String content = Optional.ofNullable(message.getContent()).orElse("");
        if (assistant) {
            Markdown markdown = new Markdown(content);
            markdown.addClassName("ai-timeline-markdown");
            return markdown;
        }

        Span text = new Span(content);
        text.addClassName("ai-timeline-user-text");
        return text;
    }
}
