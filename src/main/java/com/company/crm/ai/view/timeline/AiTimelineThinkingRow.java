package com.company.crm.ai.view.timeline;

import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.app.icons.CrmIcons;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

public class AiTimelineThinkingRow extends HorizontalLayout {

    public AiTimelineThinkingRow() {
        setWidthFull();
        setSpacing(true);
        setPadding(false);
        setAlignItems(Alignment.START);
        addClassNames("ai-timeline-message-row", "ai-timeline-message-row-assistant", "ai-timeline-message-row-thinking");
    }

    public void setThinking(TimelineItem item,
                            String assistantName,
                            String formattedTime,
                            String defaultThinkingIndicatorText) {
        removeAll();

        ChatMessage placeholder = item.message();
        List<AiUiStatusUpdate> statusUpdates = item.statusUpdates() != null ? item.statusUpdates() : List.of();

        AiTimelineAvatar assistantAvatar = new AiTimelineAvatar(true, assistantName);

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

        Span actor = new Span(assistantName);
        actor.addClassName("ai-timeline-message-actor");
        Span time = new Span(formattedTime);
        time.addClassName("ai-timeline-message-time");
        header.add(actor, time);

        Span thinkingText = buildStatusSpan(resolveActiveStatus(statusUpdates, defaultThinkingIndicatorText),
                "ai-timeline-thinking-text");

        Div shimmer = new Div();
        shimmer.addClassName("ai-timeline-thinking-shimmer");

        body.add(header, thinkingText, shimmer);
        if (statusUpdates.size() > 1) {
            body.add(createThinkingStatusList(statusUpdates));
        }

        add(assistantAvatar, body);
    }

    private Component createThinkingStatusList(List<AiUiStatusUpdate> statusUpdates) {
        VerticalLayout statusList = new VerticalLayout();
        statusList.setPadding(false);
        statusList.setSpacing(false);
        statusList.addClassName("ai-timeline-thinking-status-list");

        java.util.stream.IntStream.rangeClosed(1, statusUpdates.size() - 1)
                .mapToObj(i -> statusUpdates.get(statusUpdates.size() - 1 - i))
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

    private AiUiStatusUpdate resolveActiveStatus(List<AiUiStatusUpdate> statusUpdates, String defaultThinkingIndicatorText) {
        if (statusUpdates.isEmpty()) {
            return new AiUiStatusUpdate(defaultThinkingIndicatorText);
        }
        return statusUpdates.get(statusUpdates.size() - 1);
    }
}
