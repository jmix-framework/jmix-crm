package com.company.crm.ai.view.component.card;

import com.company.crm.ai.view.component.support.AiContextCardActionSurface;
import com.company.crm.ai.view.component.support.AiContextPendingCardLayout;
import com.company.crm.ai.view.component.support.AiContextRemoveButton;
import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.service.AiAttachmentMediaType;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.jmix.core.Messages;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class AiAttachmentCard extends Card {

    private static final Map<AiAttachmentOrigin, String> META_MESSAGE_KEYS = new EnumMap<>(AiAttachmentOrigin.class);

    static {
        META_MESSAGE_KEYS.put(AiAttachmentOrigin.AI_GENERATED, "attachmentsTypeReport");
        META_MESSAGE_KEYS.put(AiAttachmentOrigin.USER_UPLOADED, "attachmentsSourceUser");
    }

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
        VaadinIcon iconName = AiAttachmentMediaType.mediaKindFromFileName(attachment.getFileName()).getIcon();

        AiContextCardActionSurface actionSurface = new AiContextCardActionSurface();
        actionSurface.configure(
                iconName.create(),
                titleText,
                resolveMeta(attachment),
                "ai-timeline-attachment-icon",
                "ai-timeline-attachment-title",
                "ai-timeline-attachment-meta",
                attachment.getFile() != null && downloadAttachment != null
                        ? () -> downloadAttachment.accept(attachment)
                        : null
        );

        if (onRemove != null) {
            addClassName("ai-timeline-pending-card");

            AiContextRemoveButton removeButton = new AiContextRemoveButton(
                    messages.getMessage("com.company.crm.ai.view.conversation/removeContextItemAction"),
                    onRemove
            );
            add(new AiContextPendingCardLayout(actionSurface, removeButton));
        } else {
            removeClassName("ai-timeline-pending-card");
            add(actionSurface);
        }
    }

    private String resolveTitle(AiConversationAttachment attachment) {
        return Optional.ofNullable(attachment.getTitle())
                .filter(StringUtils::hasText)
                .or(() -> Optional.ofNullable(attachment.getFileName()).filter(StringUtils::hasText))
                .orElseGet(() -> messages.getMessage("com.company.crm.ai.view.conversation/attachmentsMissingFileName"));
    }

    private String resolveMeta(AiConversationAttachment attachment) {
        AiAttachmentOrigin origin = Optional.ofNullable(attachment.getOrigin()).orElse(AiAttachmentOrigin.USER_UPLOADED);
        return messages.getMessage("com.company.crm.ai.view.conversation/" + META_MESSAGE_KEYS.getOrDefault(origin, "attachmentsSourceUser"));
    }
}
