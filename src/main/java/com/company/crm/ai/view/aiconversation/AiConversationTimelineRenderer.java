package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.app.icons.CrmIcons;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.view.MessageBundle;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

class AiConversationTimelineRenderer {

    private final MessageBundle messageBundle;
    private final AiConversationContextCardFactory contextCardFactory;
    private final Function<ChatMessage, String> actorNameResolver;
    private final Function<OffsetDateTime, String> timeFormatter;
    private final Supplier<UUID> freshAssistantMessageIdSupplier;

    AiConversationTimelineRenderer(MessageBundle messageBundle,
                                   AiConversationContextCardFactory contextCardFactory,
                                   Function<ChatMessage, String> actorNameResolver,
                                   Function<OffsetDateTime, String> timeFormatter,
                                   Supplier<UUID> freshAssistantMessageIdSupplier) {
        this.messageBundle = messageBundle;
        this.contextCardFactory = contextCardFactory;
        this.actorNameResolver = actorNameResolver;
        this.timeFormatter = timeFormatter;
        this.freshAssistantMessageIdSupplier = freshAssistantMessageIdSupplier;
    }

    Component createTimelineComponent(TimelineItem item) {
        return switch (item.kind()) {
            case USER -> createMessageRow(item.message(), false);
            case ASSISTANT -> createMessageRow(item.message(), true);
            case ASSISTANT_THINKING -> createThinkingRow(item);
        };
    }

    private Component createThinkingRow(TimelineItem item) {
        ChatMessage placeholder = item.message();
        List<AiUiStatusUpdate> statusUpdates = safeStatusUpdates(item);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.START);
        row.addClassNames("ai-timeline-message-row", "ai-timeline-message-row-assistant", "ai-timeline-message-row-thinking");

        Div assistantAvatar = new Div();
        assistantAvatar.addClassNames("ai-timeline-avatar", "ai-timeline-avatar-assistant");
        assistantAvatar.add(CrmIcons.SPARKLES.create());

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setWidth("95%");
        body.addClassName("ai-timeline-message-body");

        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.addClassName("ai-timeline-message-header");

        Span actor = new Span(messageBundle.getMessage("assistantName"));
        actor.addClassName("ai-timeline-message-actor");
        Span time = new Span(timeFormatter.apply(placeholder != null ? placeholder.getCreatedDate() : null));
        time.addClassName("ai-timeline-message-time");
        header.add(actor, time);

        Span thinkingText = buildStatusSpan(resolveActiveStatus(statusUpdates),
                "ai-timeline-thinking-text");

        Div shimmer = new Div();
        shimmer.addClassName("ai-timeline-thinking-shimmer");

        body.add(header, thinkingText, shimmer);
        if (statusUpdates.size() > 1) {
            body.add(createThinkingStatusList(statusUpdates));
        }
        row.add(assistantAvatar, body);
        return row;
    }

    private Component createThinkingStatusList(List<AiUiStatusUpdate> statusUpdates) {
        VerticalLayout statusList = new VerticalLayout();
        statusList.setPadding(false);
        statusList.setSpacing(false);
        statusList.addClassName("ai-timeline-thinking-status-list");

        statusUpdates.stream()
                .limit(Math.max(0, statusUpdates.size() - 1))
                .forEach(update -> statusList.add(
                        buildStatusSpan(update, "ai-timeline-thinking-status-item")));

        return statusList;
    }

    private Span buildStatusSpan(AiUiStatusUpdate update, String mainClass) {
        Span container = new Span();
        container.addClassName(mainClass);

        Span baseText = new Span(update.message());
        baseText.addClassName("ai-timeline-thinking-status-base");
        container.add(baseText);

        if (update.isCompleted()) {
            Span resultText = new Span(" " + update.resultSnippet());
            resultText.addClassName("ai-timeline-thinking-status-result");
            container.add(resultText);
        }
        return container;
    }

    private AiUiStatusUpdate resolveActiveStatus(List<AiUiStatusUpdate> statusUpdates) {
        if (statusUpdates.isEmpty()) {
            return new AiUiStatusUpdate(messageBundle.getMessage("thinkingIndicator"));
        }
        return statusUpdates.get(statusUpdates.size() - 1);
    }

    private Component createMessageRow(ChatMessage message, boolean assistant) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.START);
        row.addClassNames("ai-timeline-message-row", assistant ? "ai-timeline-message-row-assistant" : "ai-timeline-message-row-user");
        if (assistant && isFreshAssistantMessage(message)) {
            row.addClassName("ai-timeline-message-row-fresh");
        }

        Component avatar;
        if (assistant) {
            Div assistantAvatar = new Div();
            assistantAvatar.addClassNames("ai-timeline-avatar", "ai-timeline-avatar-assistant");
            assistantAvatar.add(CrmIcons.SPARKLES.create());
            avatar = assistantAvatar;
        } else {
            Avatar userAvatar = new Avatar(actorNameResolver.apply(message));
            userAvatar.addClassName("ai-timeline-avatar");
            avatar = userAvatar;
        }

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setWidth("95%");
        body.addClassName("ai-timeline-message-body");

        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.addClassName("ai-timeline-message-header");

        Span actor = new Span(assistant ? messageBundle.getMessage("assistantName") : actorNameResolver.apply(message));
        actor.addClassName("ai-timeline-message-actor");
        Span time = new Span(timeFormatter.apply(message.getCreatedDate()));
        time.addClassName("ai-timeline-message-time");
        header.add(actor, time);

        body.add(header, createMessageContent(message, assistant));
        List<AiConversationAttachment> attachments = safeAttachments(message);
        if (!attachments.isEmpty()) {
            body.add(contextCardFactory.createAttachmentCardsGrid(attachments));
        }
        List<ChatMessageEntityReference> entityReferences = safeEntityReferences(message);
        if (!entityReferences.isEmpty()) {
            body.add(contextCardFactory.createEntityReferenceCardsGrid(entityReferences));
        }
        row.add(avatar, body);
        return row;
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

    private List<AiConversationAttachment> safeAttachments(ChatMessage message) {
        return message.getAttachments() != null ? message.getAttachments() : List.of();
    }

    private List<ChatMessageEntityReference> safeEntityReferences(ChatMessage message) {
        return message.getEntityReferences() != null ? message.getEntityReferences() : List.of();
    }

    private boolean isFreshAssistantMessage(ChatMessage message) {
        if (message == null || message.getId() == null || freshAssistantMessageIdSupplier == null) {
            return false;
        }
        UUID freshId = freshAssistantMessageIdSupplier.get();
        return freshId != null && freshId.equals(message.getId());
    }

    private List<AiUiStatusUpdate> safeStatusUpdates(TimelineItem item) {
        return item.statusUpdates() != null ? item.statusUpdates() : List.of();
    }

}
