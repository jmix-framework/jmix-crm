package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.service.AiAttachmentMediaResolver;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.Messages;

import java.util.Optional;
import java.util.function.Consumer;

public class AiAttachmentCard extends Card {

    private final Messages messages;

    public AiAttachmentCard(Messages messages) {
        this.messages = messages;

        setWidthFull();
        addThemeVariants(CardVariant.LUMO_OUTLINED);
        addClassName("ai-timeline-context-card");
    }

    public void setAttachment(AiConversationAttachment attachment,
                               Consumer<AiConversationAttachment> downloadAttachment) {
        setAttachment(attachment, downloadAttachment, null);
    }

    public void setAttachment(AiConversationAttachment attachment,
                               Consumer<AiConversationAttachment> downloadAttachment,
                               Runnable onRemove) {
        removeAll();

        String titleText = resolveTitle(attachment);
        VaadinIcon iconName = AiAttachmentMediaResolver.mediaKindFromFileName(attachment.getFileName()).getIcon();

        VerticalLayout actionSurface = new VerticalLayout();
        actionSurface.setPadding(false);
        actionSurface.setSpacing(false);
        actionSurface.setWidthFull();
        if (attachment.getFile() != null) {
            actionSurface.addClassName("ai-card-action-surface");
            actionSurface.addClickListener(event -> downloadAttachment.accept(attachment));
        }

        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setWidthFull();
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        Icon icon = iconName.create();
        icon.addClassName("ai-timeline-attachment-icon");

        Span title = new Span(titleText);
        title.addClassName("ai-timeline-attachment-title");
        title.getElement().setProperty("title", titleText);
        header.add(icon, title);
        header.expand(title);

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.setWidthFull();
        textLayout.addClassName("ai-timeline-attachment-text");

        final boolean isReport = AiAttachmentType.AI_GENERATED.equals(attachment.getType());
        final Span meta;
        if (isReport) {
            meta = new Span(messages.getMessage(getClass(), "attachmentsTypeReport"));
        } else {
            meta = new Span(resolveMeta(attachment));
        }
        meta.addClassName("ai-timeline-attachment-meta");
        textLayout.add(meta);

        actionSurface.add(header, textLayout);

        if (onRemove != null) {
            addClassName("ai-timeline-pending-card");

            Button removeButton = new Button(VaadinIcon.CLOSE.create(), event -> onRemove.run());
            // TODO: remove duplicate (siehe AiPendingEntityReferenceCard) - ggf. neue abstraktion? was ist die gemeinsamkeit? Evtl. eigene Klasse für den remove button?
            removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            removeButton.setAriaLabel(messages.getMessage(getClass(), "removeContextItemAction"));

            HorizontalLayout cardLayout = new HorizontalLayout();
            cardLayout.setPadding(false);
            cardLayout.setSpacing(true);
            cardLayout.setWidthFull();
            cardLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            cardLayout.add(actionSurface, removeButton);
            cardLayout.expand(actionSurface);

            add(cardLayout);
        } else {
            removeClassName("ai-timeline-pending-card");
            add(actionSurface);
        }
    }

    private String resolveTitle(AiConversationAttachment attachment) {
        // TODO: nicht überall ifs...
        if (attachment.getTitle() != null && !attachment.getTitle().isBlank()) {
            return attachment.getTitle();
        }
        // TODO: nicht überall ifs...
        if (attachment.getFileName() != null && !attachment.getFileName().isBlank()) {
            return attachment.getFileName();
        }
        return messages.getMessage(getClass(), "attachmentsMissingFileName");
    }

    private String resolveMeta(AiConversationAttachment attachment) {
        return switch (Optional.ofNullable(attachment.getType()).orElse(AiAttachmentType.USER_UPLOADED)) {
            // TODO: das switch als attribute in das enum verschieben
            case AI_GENERATED -> messages.getMessage(getClass(), "attachmentsSourceAi");
            case USER_UPLOADED -> messages.getMessage(getClass(), "attachmentsSourceUser");
        };
    }
}
