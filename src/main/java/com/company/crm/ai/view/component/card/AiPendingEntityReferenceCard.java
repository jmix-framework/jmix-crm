package com.company.crm.ai.view.component.card;

import com.company.crm.ai.view.component.support.AiContextCardActionSurface;
import com.company.crm.ai.view.component.support.AiContextPendingCardLayout;
import com.company.crm.ai.view.component.support.AiContextRemoveButton;
import com.company.crm.ai.service.AiEntityReferenceResolver;
import com.company.crm.ai.service.EntityReferenceViewData;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
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
                messages.getMessage("com.company.crm.ai.view.conversation/removeContextItemAction"),
                () -> onRemove.accept(entityReference)
        );

        add(new AiContextPendingCardLayout(actionSurface, removeButton));
    }
}
