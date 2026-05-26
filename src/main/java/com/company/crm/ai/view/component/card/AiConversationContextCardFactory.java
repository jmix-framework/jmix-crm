package com.company.crm.ai.view.component.card;

import com.company.crm.ai.model.AiAttachmentOrigin;
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

public class AiConversationContextCardFactory {

    private final UiComponents uiComponents;
    private final Metadata metadata;
    private final Consumer<AiConversationAttachment> downloadAttachment;
    private final Consumer<String> openCrmEntityDetail;

    public AiConversationContextCardFactory(UiComponents uiComponents,
                                     Metadata metadata,
                                     Consumer<AiConversationAttachment> downloadAttachment,
                                     Consumer<String> openCrmEntityDetail) {
        this.uiComponents = uiComponents;
        this.metadata = metadata;
        this.downloadAttachment = downloadAttachment;
        this.openCrmEntityDetail = openCrmEntityDetail;
    }

    public Component createAttachmentCardsGrid(List<AiConversationAttachment> attachments) {
        return createContextCardsGrid(attachments, new ComponentRenderer<>(this::createAttachmentCard));
    }

    public Component createEntityReferenceCardsGridFromIds(List<String> entityReferences) {
        return createContextCardsGrid(entityReferences, new ComponentRenderer<>(this::createEntityReferenceCard));
    }

    public Component createMessageContextCardsGrid(List<ChatMessageEntityReference> entityReferences,
                                                   List<AiConversationAttachment> attachments) {
        List<Object> combined = new java.util.ArrayList<>();
        combined.addAll(entityReferences != null ? entityReferences : List.of());
        combined.addAll(attachments != null ? attachments : List.of());

        return createContextCardsGrid(combined, new ComponentRenderer<>(item -> {
            if (item instanceof ChatMessageEntityReference entityReference) {
                return createEntityReferenceCard(entityReference.getEntityReference());
            } else if (item instanceof AiConversationAttachment attachment) {
                return createAttachmentCard(attachment);
            }
            return null;
        }));
    }

    public Component createPendingContextCardsGrid(List<String> entityReferences, Consumer<String> onRemoveEntity,
                                                   List<PendingAttachmentInput> attachments, Consumer<PendingAttachmentInput> onRemoveAttachment) {
        List<Object> combined = new java.util.ArrayList<>();
        combined.addAll(entityReferences);
        combined.addAll(attachments);

        return createContextCardsGrid(combined, new ComponentRenderer<>(item -> {
            if (item instanceof String entityRef) {
                return createPendingEntityReferenceCard(entityRef, onRemoveEntity);
            } else if (item instanceof PendingAttachmentInput pendingAttachment) {
                return createPendingAttachmentCard(pendingAttachment, onRemoveAttachment);
            }
            return null;
        }));
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
        attachment.setOrigin(AiAttachmentOrigin.USER_UPLOADED);
        card.setAttachment(attachment, downloadAttachment, () -> onRemove.accept(pendingAttachment));
        return card;
    }

    private Card createPendingEntityReferenceCard(String entityReference, Consumer<String> onRemove) {
        AiPendingEntityReferenceCard card = uiComponents.create(AiPendingEntityReferenceCard.class);
        card.setPendingEntityReference(entityReference, openCrmEntityDetail, onRemove);
        return card;
    }
}
