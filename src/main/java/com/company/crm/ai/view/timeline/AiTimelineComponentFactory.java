package com.company.crm.ai.view.timeline;

import com.company.crm.ai.view.component.card.AiConversationContextCardFactory;
import com.company.crm.ai.model.ChatMessage;
import com.vaadin.flow.component.Component;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.view.MessageBundle;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class AiTimelineComponentFactory {

    private final UiComponents uiComponents;
    private final MessageBundle messageBundle;
    private final AiConversationContextCardFactory contextCardFactory;
    private final Function<ChatMessage, String> actorNameResolver;
    private final Function<OffsetDateTime, String> timeFormatter;
    private final Supplier<UUID> freshAssistantMessageIdSupplier;

    public AiTimelineComponentFactory(UiComponents uiComponents,
                               MessageBundle messageBundle,
                               AiConversationContextCardFactory contextCardFactory,
                               Function<ChatMessage, String> actorNameResolver,
                               Function<OffsetDateTime, String> timeFormatter,
                               Supplier<UUID> freshAssistantMessageIdSupplier) {
        this.uiComponents = uiComponents;
        this.messageBundle = messageBundle;
        this.contextCardFactory = contextCardFactory;
        this.actorNameResolver = actorNameResolver;
        this.timeFormatter = timeFormatter;
        this.freshAssistantMessageIdSupplier = freshAssistantMessageIdSupplier;
    }

    public Component createTimelineComponent(TimelineItem item) {
        return switch (item.kind()) {
            case USER -> createMessageRow(item.message(), false);
            case ASSISTANT -> createMessageRow(item.message(), true);
            case ASSISTANT_THINKING -> createThinkingRow(item);
        };
    }

    private Component createThinkingRow(TimelineItem item) {
        AiTimelineThinkingRow row = uiComponents.create(AiTimelineThinkingRow.class);
        row.setThinking(
                item,
                messageBundle.getMessage("assistantName"),
                timeFormatter.apply(item.message() != null ? item.message().getCreatedDate() : null),
                messageBundle.getMessage("thinkingIndicator")
        );
        return row;
    }

    private Component createMessageRow(ChatMessage message, boolean assistant) {
        AiTimelineMessageRow row = uiComponents.create(AiTimelineMessageRow.class);
        row.setMessage(
                message,
                assistant,
                isFreshAssistantMessage(message),
                assistant ? messageBundle.getMessage("assistantName") : actorNameResolver.apply(message),
                timeFormatter.apply(message.getCreatedDate()),
                contextCardFactory
        );
        return row;
    }

    private boolean isFreshAssistantMessage(ChatMessage message) {
        UUID freshId = freshAssistantMessageIdSupplier.get();
        return freshId != null && freshId.equals(message.getId());
    }
}
