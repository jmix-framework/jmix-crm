package com.company.crm.ai.view.conversation.component;

import com.company.crm.ai.model.AiConversation;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class AiConversationHistoryGroup extends VerticalLayout {

    public AiConversationHistoryGroup() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("ai-conversation-starter-history-group");
    }

    public void setGroup(String bucketLabel,
                         List<AiConversation> conversations,
                         Function<AiConversation, Card> cardCreator) {
        HorizontalLayout groupHeader = new HorizontalLayout();
        groupHeader.setWidthFull();
        groupHeader.setAlignItems(Alignment.CENTER);
        groupHeader.addClassName("ai-conversation-starter-history-group-header");

        Span bucket = new Span();
        bucket.setText(bucketLabel.toUpperCase(Locale.ROOT));
        bucket.addClassNames("text-secondary", "text-xs", "font-semibold",
                "ai-conversation-starter-history-bucket");

        Span groupCount = new Span();
        groupCount.setText(String.valueOf(conversations.size()));
        groupCount.addClassNames("text-secondary", "text-xs",
                "ai-conversation-starter-history-group-count");

        groupHeader.add(bucket, groupCount);
        add(groupHeader);

        conversations.stream()
                .map(cardCreator)
                .forEach(this::add);
    }
}
