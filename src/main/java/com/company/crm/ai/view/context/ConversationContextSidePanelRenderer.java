package com.company.crm.ai.view.context;

import com.company.crm.ai.view.component.card.AiConversationContextCardFactory;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.sidepanellayout.SidePanelLayout;
import io.jmix.flowui.view.MessageBundle;

import java.util.List;

public class ConversationContextSidePanelRenderer {

    private final MessageBundle messageBundle;
    private final ConversationContextAggregator aggregator;
    private final AiConversationContextCardFactory cardFactory;

    public ConversationContextSidePanelRenderer(
                                         MessageBundle messageBundle,
                                         ConversationContextAggregator aggregator,
                                         AiConversationContextCardFactory cardFactory) {
        this.messageBundle = messageBundle;
        this.aggregator = aggregator;
        this.cardFactory = cardFactory;
    }

    void render(VerticalLayout container, SidePanelLayout sidePanelLayout, AiConversation conversation) {
        container.removeAll();

        ConversationContextItems items = aggregator.aggregate(conversation);

        container.add(createHeader(sidePanelLayout, items));
        container.add(createContent(items));
    }

    private Component createHeader(SidePanelLayout sidePanelLayout, ConversationContextItems items) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("ai-conversation-context-header");

        H4 title = new H4(messageBundle.getMessage("contextSidePanelTitle"));
        title.addClassName("ai-conversation-context-header-title");

        Span count = new Span(String.valueOf(items.totalCount()));
        count.addClassName("ai-conversation-context-header-count");

        HorizontalLayout titleGroup = new HorizontalLayout(title, count);
        titleGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        titleGroup.setSpacing(true);
        titleGroup.setPadding(false);

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> sidePanelLayout.closeSidePanel());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel(messageBundle.getMessage("contextSidePanelClose"));

        header.add(titleGroup, closeButton);
        header.expand(titleGroup);
        return header;
    }

    private Component createContent(ConversationContextItems items) {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setPadding(false);
        content.setSpacing(true);
        content.addClassName("ai-conversation-context-content");

        if (items.isEmpty()) {
            Span emptyHint = new Span(messageBundle.getMessage("contextSidePanelEmpty"));
            emptyHint.addClassName("ai-conversation-context-empty");
            content.add(emptyHint);
            return content;
        }

        addEntityReferencesSection(content, items.entityReferences());
        addAttachmentsSection(content, "contextSectionGenerated", items.generatedAttachments());
        addAttachmentsSection(content, "contextSectionUploaded", items.uploadedAttachments());

        return content;
    }

    private void addEntityReferencesSection(VerticalLayout parent, List<String> entityReferences) {
        if (entityReferences.isEmpty()) {
            return;
        }
        parent.add(createSectionHeading("contextSectionEntitiesReferenced", entityReferences.size()));
        parent.add(cardFactory.createEntityReferenceCardsGridFromIds(entityReferences));
    }

    private void addAttachmentsSection(VerticalLayout parent,
                                       String headingKey,
                                       List<AiConversationAttachment> attachments) {
        if (attachments.isEmpty()) {
            return;
        }
        parent.add(createSectionHeading(headingKey, attachments.size()));
        parent.add(cardFactory.createAttachmentCardsGrid(attachments));
    }

    private Component createSectionHeading(String messageKey, int count) {
        HorizontalLayout heading = new HorizontalLayout();
        heading.setWidthFull();
        heading.setAlignItems(FlexComponent.Alignment.CENTER);
        heading.setSpacing(true);
        heading.setPadding(false);
        heading.addClassName("ai-conversation-context-section-heading");

        H5 title = new H5(messageBundle.getMessage(messageKey));
        title.addClassName("ai-conversation-context-section-title");

        Span badge = new Span(String.valueOf(count));
        badge.addClassName("ai-conversation-context-section-count");

        heading.add(title, badge);
        return heading;
    }
}
