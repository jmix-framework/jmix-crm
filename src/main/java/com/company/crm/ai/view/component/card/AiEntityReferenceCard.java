package com.company.crm.ai.view.component.card;

import com.company.crm.ai.view.component.support.AiContextCardActionSurface;
import com.company.crm.ai.service.AiEntityReferenceResolver;
import com.company.crm.ai.service.EntityReferenceViewData;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;

import java.util.function.Consumer;

public class AiEntityReferenceCard extends Card {

    private final AiEntityReferenceResolver entityReferenceResolver;

    public AiEntityReferenceCard(AiEntityReferenceResolver entityReferenceResolver) {
        this.entityReferenceResolver = entityReferenceResolver;

        setWidthFull();
        addThemeVariants(CardVariant.LUMO_OUTLINED);
        addClassName("ai-timeline-context-card");
    }

    public void setEntityReference(String entityReference,
                                   Consumer<String> openCrmEntityDetail) {
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

        add(actionSurface);
    }
}
