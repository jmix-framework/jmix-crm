package com.company.crm.ai.view.conversation.component;

import com.company.crm.ai.view.component.support.AiContextCardActionSurface;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.app.icons.CrmIcons;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;

import java.time.OffsetDateTime;
import java.util.Optional;
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
        removeAll();

        AiContextCardActionSurface actionSurface = new AiContextCardActionSurface();
        actionSurface.configure(
                CrmIcons.SPARKLES.create(),
                conversation.getInstanceName(),
                formattedCreatedDate(conversation, dateTimeFormatter),
                "ai-conversation-starter-conversation-card-icon",
                "font-semibold text-m ai-conversation-starter-conversation-card-title",
                "text-s text-secondary",
                () -> selectionHandler.accept(conversation)
        );

        add(actionSurface);
    }

    private String formattedCreatedDate(AiConversation conversation,
                                        Function<OffsetDateTime, String> dateTimeFormatter) {
        return Optional.ofNullable(conversation.getCreatedDate())
                .map(dateTimeFormatter)
                .orElse(null);
    }
}
