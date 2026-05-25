package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.service.AiEntityReferenceResolver;
import com.company.crm.ai.service.EntityReferenceViewData;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.core.Messages;

import java.util.function.Consumer;

public class AiPendingEntityReferenceCard extends Card {

    private final Messages messages;
    private final AiEntityReferenceResolver entityReferenceResolver;

    public AiPendingEntityReferenceCard(Messages messages,
                                         AiEntityReferenceResolver entityReferenceResolver) {
        this.messages = messages;
        this.entityReferenceResolver = entityReferenceResolver;

        setWidthFull();
        addThemeVariants(CardVariant.LUMO_OUTLINED);
        addClassName("ai-timeline-context-card");
        addClassName("ai-timeline-pending-card");
    }

    public void setPendingEntityReference(String entityReference,
                                           Consumer<String> openCrmEntityDetail,
                                           Consumer<String> onRemove) {
        EntityReferenceViewData viewData = entityReferenceResolver.resolve(entityReference);

        AiContextCardActionSurface actionSurface = new AiContextCardActionSurface();
        actionSurface.configure(
                viewData.icon().create(),
                viewData.title(),
                viewData.meta(),
                "ai-timeline-attachment-icon",
                "ai-timeline-attachment-title",
                "ai-timeline-attachment-meta",
                () -> openCrmEntityDetail.accept(entityReference)
        );

        AiContextRemoveButton removeButton = new AiContextRemoveButton(
                messages.getMessage(getClass(), "removeContextItemAction"),
                () -> onRemove.accept(entityReference)
        );

        HorizontalLayout cardLayout = new HorizontalLayout();
        cardLayout.setPadding(false);
        cardLayout.setSpacing(true);
        cardLayout.setWidthFull();
        cardLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        cardLayout.add(actionSurface, removeButton);
        cardLayout.expand(actionSurface);

        add(cardLayout);
    }
}
