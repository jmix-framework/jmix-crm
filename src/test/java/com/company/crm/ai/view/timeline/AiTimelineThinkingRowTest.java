package com.company.crm.ai.view.timeline;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.UiComponents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiTimelineThinkingRowTest extends AbstractUiTest {

    @Autowired
    private UiComponents uiComponents;

    @Test
    void rendersProgressShimmerAndCompletedToolStatusListItems() {
        TimelineItem item = TimelineItem.thinking(placeholderMessage());
        item.statusUpdates().add(new AiUiStatusUpdate("Looking up invoices", "3 invoices"));
        item.statusUpdates().add(new AiUiStatusUpdate("Checking payments", "2 payments"));
        item.statusUpdates().add(new AiUiStatusUpdate("Writing answer"));

        AiTimelineThinkingRow row = uiComponents.create(AiTimelineThinkingRow.class);
        row.setThinking(item, "CRM AI", "09:31", "Thinking...");

        assertThat(viewTestSupport.descendants(row, Div.class)
                .filter(div -> div.getClassNames().contains("ai-timeline-thinking-shimmer"))
                .findFirst())
                .isPresent();

        VerticalLayout statusList = viewTestSupport.descendants(row, VerticalLayout.class)
                .filter(layout -> layout.getClassNames().contains("ai-timeline-thinking-status-list"))
                .findFirst()
                .orElseThrow();
        List<Span> statusItems = viewTestSupport.descendants(statusList, Span.class)
                .filter(span -> span.getClassNames().contains("ai-timeline-thinking-status-item"))
                .toList();

        assertThat(statusItems).hasSize(2);
        assertThat(statusItems).allSatisfy(itemSpan ->
                assertThat(itemSpan.getClassNames()).contains("ai-timeline-thinking-status-item"));
        assertThat(viewTestSupport.textsByClassName(statusList, "ai-timeline-thinking-status-base"))
                .containsExactly("✓ Checking payments", "✓ Looking up invoices");
        assertThat(viewTestSupport.textsByClassName(statusList, "ai-timeline-thinking-status-result"))
                .containsExactly(" 2 payments", " 3 invoices");
    }

    private static ChatMessage placeholderMessage() {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setType(ChatMessageType.ASSISTANT);
        message.setCreatedDate(OffsetDateTime.now());
        return message;
    }
}
