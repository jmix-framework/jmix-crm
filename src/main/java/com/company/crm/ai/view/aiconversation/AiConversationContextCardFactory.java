package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.Metadata;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.gridlayout.GridLayout;

import java.util.List;
import java.util.function.Consumer;

class AiConversationContextCardFactory {

    private final UiComponents uiComponents;
    private final Metadata metadata;
    private final Consumer<AiConversationAttachment> downloadAttachment;
    private final Consumer<String> openCrmEntityDetail;

    AiConversationContextCardFactory(UiComponents uiComponents,
                                     Metadata metadata,
                                     Consumer<AiConversationAttachment> downloadAttachment,
                                     Consumer<String> openCrmEntityDetail) {
        this.uiComponents = uiComponents;
        this.metadata = metadata;
        this.downloadAttachment = downloadAttachment;
        this.openCrmEntityDetail = openCrmEntityDetail;
    }

    Component createAttachmentCardsGrid(List<AiConversationAttachment> attachments) {
        return createContextCardsGrid(attachments, new ComponentRenderer<>(this::createAttachmentCard));
    }

    Component createEntityReferenceCardsGrid(List<ChatMessageEntityReference> entityReferences) {
        return createContextCardsGrid(entityReferences, new ComponentRenderer<>(
                reference -> createEntityReferenceCard(reference.getEntityReference())));
    }

    Component createEntityReferenceCardsGridFromIds(List<String> entityReferences) {
        return createContextCardsGrid(entityReferences, new ComponentRenderer<>(this::createEntityReferenceCard));
    }

    Component createPendingEntityReferenceCardsGrid(List<String> entityReferences, Consumer<String> onRemove) {
        return createContextCardsGrid(entityReferences, new ComponentRenderer<>(entityReference -> createPendingEntityReferenceCard(entityReference, onRemove)));
    }

    Component createPendingAttachmentCardsGrid(List<PendingAttachmentInput> attachments, Consumer<PendingAttachmentInput> onRemove) {
        return createContextCardsGrid(attachments, new ComponentRenderer<>(pendingAttachment -> createPendingAttachmentCard(pendingAttachment, onRemove)));
    }

    private <T> GridLayout<T> createContextCardsGrid(List<T> items, ComponentRenderer<Card, T> renderer) {
        @SuppressWarnings("unchecked")
        GridLayout<T> cards = uiComponents.create(GridLayout.class);
        cards.addClassName("ai-timeline-context-cards");
        cards.setRenderer(renderer);
        cards.setItems(items);
        return cards;
    }

    private Card createAttachmentCard(AiConversationAttachment attachment) {
        AiAttachmentCard card = uiComponents.create(AiAttachmentCard.class);
        card.setAttachment(attachment, downloadAttachment);
        return card;
    }

    private Card createEntityReferenceCard(String entityReference) {
        AiEntityReferenceCard card = uiComponents.create(AiEntityReferenceCard.class);
        card.setEntityReference(entityReference, openCrmEntityDetail);
        return card;
    }

    private Card createPendingAttachmentCard(PendingAttachmentInput pendingAttachment, Consumer<PendingAttachmentInput> onRemove) {
        AiAttachmentCard card = uiComponents.create(AiAttachmentCard.class);
        AiConversationAttachment attachment = metadata.create(AiConversationAttachment.class);
        attachment.setFile(pendingAttachment.fileRef());
        attachment.setFileName(pendingAttachment.fileName());
        attachment.setTitle(pendingAttachment.fileName());
        attachment.setType(AiAttachmentType.USER_UPLOADED);
        card.setAttachment(attachment, downloadAttachment, () -> onRemove.accept(pendingAttachment));
        return card;
    }

    private Card createPendingEntityReferenceCard(String entityReference, Consumer<String> onRemove) {
        AiPendingEntityReferenceCard card = uiComponents.create(AiPendingEntityReferenceCard.class);
        card.setPendingEntityReference(entityReference, openCrmEntityDetail, onRemove);
        return card;
    }
}
