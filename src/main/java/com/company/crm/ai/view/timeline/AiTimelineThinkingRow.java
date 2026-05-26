package com.company.crm.ai.view.timeline;

import com.company.crm.ai.model.AiUiStatusUpdate;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

public class AiTimelineThinkingRow extends AbstractAiTimelineRow {

    public AiTimelineThinkingRow() {
        super();
        addClassNames("ai-timeline-message-row", "ai-timeline-message-row-assistant", "ai-timeline-message-row-thinking");
    }

    public void setThinking(TimelineItem item,
                            String assistantName,
                            String formattedTime,
                            String defaultThinkingIndicatorText) {
        List<AiUiStatusUpdate> statusUpdates = item.statusUpdates() != null ? item.statusUpdates() : List.of();

        initRow(true, assistantName, formattedTime);

        Span thinkingText = buildStatusSpan(resolveActiveStatus(statusUpdates, defaultThinkingIndicatorText)
        );

        Div shimmer = new Div();
        shimmer.addClassName("ai-timeline-thinking-shimmer");

        body.add(thinkingText, shimmer);
        if (statusUpdates.size() > 1) {
            body.add(createThinkingStatusList(statusUpdates));
        }
    }

    private Component createThinkingStatusList(List<AiUiStatusUpdate> statusUpdates) {
        VerticalLayout statusList = new VerticalLayout();
        statusList.setPadding(false);
        statusList.setSpacing(false);
        statusList.addClassName("ai-timeline-thinking-status-list");

        java.util.stream.IntStream.rangeClosed(1, statusUpdates.size() - 1)
                .mapToObj(i -> statusUpdates.get(statusUpdates.size() - 1 - i))
                .forEach(update -> statusList.add(
                        buildStatusSpan(update, "ai-timeline-thinking-status-item", true)));

        return statusList;
    }

    private Span buildStatusSpan(AiUiStatusUpdate update) {
        return buildStatusSpan(update, "ai-timeline-thinking-text", false);
    }

    private Span buildStatusSpan(AiUiStatusUpdate update, String mainClass, boolean completedPrefix) {
        Span container = new Span();
        container.addClassName(mainClass);

        String prefix = completedPrefix && update.isCompleted() ? "✓ " : "";
        Span baseText = new Span(prefix + update.message());
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
        return statusUpdates.getLast();
    }
}
