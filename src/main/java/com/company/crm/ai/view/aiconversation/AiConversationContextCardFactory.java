package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.service.AiAttachmentMediaResolver;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.MessageTools;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.gridlayout.GridLayout;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.MessageBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

class AiConversationContextCardFactory {

    private static final Logger log = LoggerFactory.getLogger(AiConversationContextCardFactory.class);

    private final UiComponents uiComponents;
    private final DataManager dataManager;
    private final IdSerialization idSerialization;
    private final Metadata metadata;
    private final MetadataTools metadataTools;
    private final MessageTools messageTools;
    private final MessageBundle messageBundle;
    private final Consumer<AiConversationAttachment> downloadAttachment;
    private final Consumer<String> openCrmEntityDetail;
    private final AiContextEntityRegistry contextEntityRegistry;

    AiConversationContextCardFactory(UiComponents uiComponents,
                                     DataManager dataManager,
                                     IdSerialization idSerialization,
                                     Metadata metadata,
                                     MetadataTools metadataTools,
                                     MessageTools messageTools,
                                     MessageBundle messageBundle,
                                     Consumer<AiConversationAttachment> downloadAttachment,
                                     Consumer<String> openCrmEntityDetail,
                                     AiContextEntityRegistry contextEntityRegistry) {
        this.uiComponents = uiComponents;
        this.dataManager = dataManager;
        this.idSerialization = idSerialization;
        this.metadata = metadata;
        this.metadataTools = metadataTools;
        this.messageTools = messageTools;
        this.messageBundle = messageBundle;
        this.downloadAttachment = downloadAttachment;
        this.openCrmEntityDetail = openCrmEntityDetail;
        this.contextEntityRegistry = contextEntityRegistry;
    }

    @SuppressWarnings("unchecked")
    Component createAttachmentCardsGrid(List<AiConversationAttachment> attachments) {
        return createContextCardsGrid(attachments, new ComponentRenderer<>(this::createAttachmentCard));
    }

    @SuppressWarnings("unchecked")
    Component createEntityReferenceCardsGrid(List<ChatMessageEntityReference> entityReferences) {
        return createContextCardsGrid(entityReferences, new ComponentRenderer<>(
                reference -> createEntityReferenceCard(reference.getEntityReference())));
    }

    @SuppressWarnings("unchecked")
    Component createEntityReferenceCardsGridFromIds(List<String> entityReferences) {
        return createContextCardsGrid(entityReferences, new ComponentRenderer<>(this::createEntityReferenceCard));
    }

    @SuppressWarnings("unchecked")
    Component createPendingEntityReferenceCardsGrid(List<String> entityReferences, Consumer<String> onRemove) {
        return createContextCardsGrid(entityReferences, new ComponentRenderer<>(entityReference -> createPendingEntityReferenceCard(entityReference, onRemove)));
    }

    @SuppressWarnings("unchecked")
    Component createPendingAttachmentCardsGrid(List<PendingAttachmentInput> attachments, Consumer<PendingAttachmentInput> onRemove) {
        return createContextCardsGrid(attachments, new ComponentRenderer<>(pendingAttachment -> createPendingAttachmentCard(pendingAttachment, onRemove)));
    }

    private <T> GridLayout<T> createContextCardsGrid(List<T> items, ComponentRenderer<Card, T> renderer) {
        @SuppressWarnings("unchecked")
        GridLayout<T> cards = uiComponents.create(GridLayout.class);
        cards.setWidthFull();
        cards.setColumnMinWidth("18rem");
        cards.setGap("var(--lumo-space-s)");
        cards.addClassName("ai-timeline-context-cards");
        cards.setRenderer(renderer);
        cards.setItems(items);
        return cards;
    }

    private VerticalLayout populateCardHeaderAndTextLayout(Card card, VaadinIcon iconName, String titleText) {
        Icon icon = iconName.create();
        icon.addClassName("ai-timeline-attachment-icon");
        card.setHeaderPrefix(icon);

        Span title = new Span(titleText);
        title.addClassName("ai-timeline-attachment-title");
        title.getElement().setProperty("title", titleText);
        card.setTitle(title);

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.setWidthFull();
        textLayout.addClassName("ai-timeline-attachment-text");
        return textLayout;
    }

    private JmixButton createOpenButton(boolean enabled, ComponentEventListener<ClickEvent<Button>> clickListener) {
        JmixButton openButton = uiComponents.create(JmixButton.class);
        openButton.setIcon(VaadinIcon.EXTERNAL_LINK.create());
        openButton.addThemeNames("tertiary-inline", "small", "icon");
        openButton.setAriaLabel(messageBundle.getMessage("attachmentsDownloadAction"));
        openButton.setEnabled(enabled);
        openButton.addClickListener(clickListener);
        return openButton;
    }

    private Card createAttachmentCard(AiConversationAttachment attachment) {
        Card card = uiComponents.create(Card.class);
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_OUTLINED);
        card.addClassName("ai-timeline-context-card");
        if (attachment.getFile() != null) {
            card.getStyle().set("cursor", "pointer");
            addCardClickListener(card, () -> downloadAttachment.accept(attachment));
        }

        boolean isReport = AiAttachmentType.AI_GENERATED.equals(attachment.getType());

        VerticalLayout textLayout = populateCardHeaderAndTextLayout(
                card, 
                resolveIcon(attachment), 
                resolveAttachmentTitle(attachment)
        );

        if (!isReport) {
            Span meta = new Span(attachmentMeta(attachment));
            meta.addClassName("ai-timeline-attachment-meta");
            textLayout.add(meta);
        }

        JmixButton openButton = createOpenButton(
                attachment.getFile() != null, 
                click -> downloadAttachment.accept(attachment)
        );
        card.setHeaderSuffix(openButton);

        card.add(textLayout);
        return card;
    }

    private Card createEntityReferenceCard(String entityReference) {
        Card card = uiComponents.create(Card.class);
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_OUTLINED);
        card.addClassName("ai-timeline-context-card");
        card.getStyle().set("cursor", "pointer");
        addCardClickListener(card, () -> openCrmEntityDetail.accept(entityReference));

        EntityReferenceViewData viewData = entityReferenceViewData(entityReference);

        VerticalLayout textLayout = populateCardHeaderAndTextLayout(
                card, 
                viewData.icon(), 
                viewData.title()
        );

        Span meta = new Span(viewData.meta());
        meta.addClassName("ai-timeline-attachment-meta");
        textLayout.add(meta);

        JmixButton openButton = createOpenButton(
                true, 
                click -> openCrmEntityDetail.accept(entityReference)
        );
        card.setHeaderSuffix(openButton);

        card.add(textLayout);
        return card;
    }

    private Card createPendingAttachmentCard(PendingAttachmentInput pendingAttachment, Consumer<PendingAttachmentInput> onRemove) {
        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setFile(pendingAttachment.fileRef());
        attachment.setFileName(pendingAttachment.fileName());
        attachment.setTitle(pendingAttachment.fileName());
        attachment.setType(AiAttachmentType.USER_UPLOADED);

        Card card = uiComponents.create(Card.class);
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_OUTLINED);
        card.addClassName("ai-timeline-context-card");
        card.addClassName("ai-timeline-pending-card");
        if (attachment.getFile() != null) {
            card.getStyle().set("cursor", "pointer");
            addCardClickListener(card, () -> downloadAttachment.accept(attachment));
        }

        VerticalLayout textLayout = populateCardHeaderAndTextLayout(
                card, 
                resolveIcon(attachment), 
                resolveAttachmentTitle(attachment)
        );

        Span meta = new Span(attachmentMeta(attachment));
        meta.addClassName("ai-timeline-attachment-meta");
        textLayout.add(meta);

        JmixButton openButton = createOpenButton(
                attachment.getFile() != null, 
                click -> downloadAttachment.accept(attachment)
        );

        JmixButton removeButton = uiComponents.create(JmixButton.class);
        removeButton.setIcon(VaadinIcon.CLOSE.create());
        removeButton.addThemeNames("tertiary-inline", "small", "icon");
        removeButton.setAriaLabel(messageBundle.getMessage("removeContextItemAction"));
        removeButton.addClickListener(click -> onRemove.accept(pendingAttachment));
        removeButton.getElement().executeJs("this.addEventListener('click', e => e.stopPropagation());");

        HorizontalLayout suffixLayout = uiComponents.create(HorizontalLayout.class);
        suffixLayout.setPadding(false);
        suffixLayout.setSpacing(true);
        suffixLayout.add(openButton, removeButton);
        card.setHeaderSuffix(suffixLayout);

        card.add(textLayout);
        return card;
    }

    private Card createPendingEntityReferenceCard(String entityReference, Consumer<String> onRemove) {
        Card card = uiComponents.create(Card.class);
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_OUTLINED);
        card.addClassName("ai-timeline-context-card");
        card.addClassName("ai-timeline-pending-card");
        card.getStyle().set("cursor", "pointer");
        addCardClickListener(card, () -> openCrmEntityDetail.accept(entityReference));

        EntityReferenceViewData viewData = entityReferenceViewData(entityReference);

        VerticalLayout textLayout = populateCardHeaderAndTextLayout(
                card, 
                viewData.icon(), 
                viewData.title()
        );

        Span meta = new Span(viewData.meta());
        meta.addClassName("ai-timeline-attachment-meta");
        textLayout.add(meta);

        JmixButton openButton = createOpenButton(
                true, 
                click -> openCrmEntityDetail.accept(entityReference)
        );

        JmixButton removeButton = uiComponents.create(JmixButton.class);
        removeButton.setIcon(VaadinIcon.CLOSE.create());
        removeButton.addThemeNames("tertiary-inline", "small", "icon");
        removeButton.setAriaLabel(messageBundle.getMessage("removeContextItemAction"));
        removeButton.addClickListener(click -> onRemove.accept(entityReference));
        removeButton.getElement().executeJs("this.addEventListener('click', e => e.stopPropagation());");

        HorizontalLayout suffixLayout = uiComponents.create(HorizontalLayout.class);
        suffixLayout.setPadding(false);
        suffixLayout.setSpacing(true);
        suffixLayout.add(openButton, removeButton);
        card.setHeaderSuffix(suffixLayout);

        card.add(textLayout);
        return card;
    }

    private void addCardClickListener(Card card, Runnable action) {
        card.getElement().addEventListener("click", event -> action.run());
    }

    private EntityReferenceViewData entityReferenceViewData(String entityReference) {
        if (entityReference == null || entityReference.isBlank()) {
            return fallbackEntityReferenceViewData();
        }

        try {
            Id<Object> id = idSerialization.stringToId(entityReference);
            Object entity = dataManager.load(id).one();
            String title = Optional.ofNullable(metadataTools.getInstanceName(entity))
                    .filter(name -> !name.isBlank())
                    .orElseGet(() -> id.getEntityClass().getSimpleName());

            VaadinIcon icon = contextEntityRegistry.findDefinition(id.getEntityClass())
                    .map(definition -> definition.icon())
                    .orElse(VaadinIcon.DATABASE);
            MetaClass metaClass = metadata.getClass(id.getEntityClass());
            String caption = messageTools.getEntityCaption(metaClass);

            return new EntityReferenceViewData(title, caption, icon);
        } catch (Exception e) {
            log.warn("Failed to resolve CRM entity reference {}", entityReference, e);
            return fallbackEntityReferenceViewData();
        }
    }

    private EntityReferenceViewData fallbackEntityReferenceViewData() {
        return new EntityReferenceViewData(
                messageBundle.getMessage("entityReferenceFallbackTitle"),
                messageBundle.getMessage("entityReferenceUnavailable"),
                VaadinIcon.DATABASE
        );
    }

    private String resolveAttachmentTitle(AiConversationAttachment attachment) {
        if (attachment.getTitle() != null && !attachment.getTitle().isBlank()) {
            return attachment.getTitle();
        }
        if (attachment.getFileName() != null && !attachment.getFileName().isBlank()) {
            return attachment.getFileName();
        }
        return messageBundle.getMessage("attachmentsMissingFileName");
    }

    private String attachmentMeta(AiConversationAttachment attachment) {
        return switch (Optional.ofNullable(attachment.getType()).orElse(AiAttachmentType.USER_UPLOADED)) {
            case AI_GENERATED -> messageBundle.getMessage("attachmentsSourceAi");
            case USER_UPLOADED -> messageBundle.getMessage("attachmentsSourceUser");
        };
    }

    private VaadinIcon resolveIcon(AiConversationAttachment attachment) {
        return switch (AiAttachmentMediaResolver.mediaKindFromFileName(attachment.getFileName())) {
            case SPREADSHEET -> VaadinIcon.TABLE;
            case TEXT_DOCUMENT -> VaadinIcon.FILE_TEXT_O;
            case JSON -> VaadinIcon.CODE;
            case IMAGE, OTHER -> VaadinIcon.FILE_O;
        };
    }

    private record EntityReferenceViewData(String title, String meta, VaadinIcon icon) {
    }
}
