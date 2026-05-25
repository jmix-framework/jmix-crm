package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.app.icons.CrmIcons;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.OffsetDateTime;
import java.util.function.Consumer;
import java.util.function.Function;

public class AiConversationCard extends Card {

    public AiConversationCard() {
        setWidthFull();
        addClassName("ai-conversation-starter-conversation-card");
        addThemeVariants(CardVariant.LUMO_OUTLINED);
    }

    public void setConversation(AiConversation conversation,
                                Consumer<AiConversation> selectionHandler,
                                Function<OffsetDateTime, String> dateTimeFormatter) {
        // TODO: duplicate code, siehe AiEntityReferenceCard. Finde common Lösung
        VerticalLayout actionSurface = new VerticalLayout();
        actionSurface.setPadding(false);
        actionSurface.setSpacing(false);
        actionSurface.setWidthFull();
        actionSurface.addClassName("ai-card-action-surface");
        actionSurface.addClickListener(event -> selectionHandler.accept(conversation));

        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setWidthFull();
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        Icon icon = CrmIcons.SPARKLES.create();
        icon.addClassName("ai-conversation-starter-conversation-card-icon");

        Span titleText = new Span();
        titleText.setText(conversation.getInstanceName());
        titleText.addClassNames("font-semibold", "text-m",
                "ai-conversation-starter-conversation-card-title");
        titleText.getElement().setProperty("title", conversation.getInstanceName());
        header.add(icon, titleText);
        header.expand(titleText);
        actionSurface.add(header);

        OffsetDateTime createdDate = conversation.getCreatedDate();
        if (createdDate != null) {
            Span subtitle = new Span();
            subtitle.setText(dateTimeFormatter.apply(createdDate));
            subtitle.addClassNames("text-s", "text-secondary");
            actionSurface.add(subtitle);
        }
        add(actionSurface);
    }
}
