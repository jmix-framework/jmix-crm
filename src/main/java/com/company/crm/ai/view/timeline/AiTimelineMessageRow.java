package com.company.crm.ai.view.timeline;

import com.company.crm.ai.view.component.card.AiConversationContextCardFactory;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.app.icons.CrmIcons;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.UiComponents;

import java.util.List;
import java.util.Optional;

public class AiTimelineMessageRow extends HorizontalLayout {

    private final UiComponents uiComponents;

    public AiTimelineMessageRow(UiComponents uiComponents) {
        this.uiComponents = uiComponents;

        setWidthFull();
        setSpacing(true);
        setPadding(false);
        setAlignItems(Alignment.START);
    }

    public void setMessage(ChatMessage message,
                           boolean assistant,
                           boolean isFresh,
                           String actorName,
                           String formattedTime,
                           AiConversationContextCardFactory contextCardFactory) {
        removeAll();
        resetMessageClassNames();

        addClassNames("ai-timeline-message-row", assistant ? "ai-timeline-message-row-assistant" : "ai-timeline-message-row-user");
        if (assistant && isFresh) {
            addClassName("ai-timeline-message-row-fresh");
        }

        AiTimelineAvatar avatar = new AiTimelineAvatar(assistant, actorName);

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setWidth("95%");
        body.addClassName("ai-timeline-message-body");

        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(Alignment.BASELINE);
        header.addClassName("ai-timeline-message-header");

        Span actor = new Span(actorName);
        actor.addClassName("ai-timeline-message-actor");
        Span time = new Span(formattedTime);
        time.addClassName("ai-timeline-message-time");
        header.add(actor, time);

        body.add(header, createMessageContent(message, assistant));

        List<AiConversationAttachment> attachments = message.getAttachments() != null ? message.getAttachments() : List.of();
        if (!attachments.isEmpty()) {
            body.add(contextCardFactory.createAttachmentCardsGrid(attachments));
        }

        List<ChatMessageEntityReference> entityReferences = message.getEntityReferences() != null ? message.getEntityReferences() : List.of();
        if (!entityReferences.isEmpty()) {
            body.add(contextCardFactory.createEntityReferenceCardsGrid(entityReferences));
        }

        add(avatar, body);
    }

    private void resetMessageClassNames() {
        removeClassName("ai-timeline-message-row-fresh");
        removeClassName("ai-timeline-message-row-assistant");
        removeClassName("ai-timeline-message-row-user");
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
