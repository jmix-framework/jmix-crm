package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.model.base.UuidEntity;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.MessageTools;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.sidepanellayout.SidePanelLayout;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class ConversationContextPanelSupport {

    private static final Logger log = LoggerFactory.getLogger(ConversationContextPanelSupport.class);

    private final UiComponents uiComponents;
    private final DataManager dataManager;
    private final IdSerialization idSerialization;
    private final Metadata metadata;
    private final MetadataTools metadataTools;
    private final MessageTools messageTools;
    private final DialogWindows dialogWindows;
    private final Notifications notifications;
    private final Downloader downloader;
    private final AiContextEntityRegistry contextEntityRegistry;
    private final ConversationContextAggregator contextAggregator = new ConversationContextAggregator();

    ConversationContextPanelSupport(UiComponents uiComponents,
                                    DataManager dataManager,
                                    IdSerialization idSerialization,
                                    Metadata metadata,
                                    MetadataTools metadataTools,
                                    MessageTools messageTools,
                                    DialogWindows dialogWindows,
                                    Notifications notifications,
                                    Downloader downloader,
                                    AiContextEntityRegistry contextEntityRegistry) {
        this.uiComponents = uiComponents;
        this.dataManager = dataManager;
        this.idSerialization = idSerialization;
        this.metadata = metadata;
        this.metadataTools = metadataTools;
        this.messageTools = messageTools;
        this.dialogWindows = dialogWindows;
        this.notifications = notifications;
        this.downloader = downloader;
        this.contextEntityRegistry = contextEntityRegistry;
    }

    AiConversationContextCardFactory contextCardFactory(View<?> origin, MessageBundle messageBundle) {
        return new AiConversationContextCardFactory(
                uiComponents,
                dataManager,
                idSerialization,
                metadata,
                metadataTools,
                messageTools,
                messageBundle,
                this::downloadAttachment,
                entityReference -> openCrmEntityDetail(origin, messageBundle, entityReference),
                contextEntityRegistry
        );
    }

    void render(View<?> origin,
                MessageBundle messageBundle,
                VerticalLayout content,
                SidePanelLayout sidePanel,
                AiConversation conversation) {
        if (content == null) {
            return;
        }

        new ConversationContextSidePanelRenderer(
                uiComponents,
                messageBundle,
                contextAggregator,
                contextCardFactory(origin, messageBundle)
        ).render(content, sidePanel, conversation);
    }

    void refreshToggleLabel(View<?> origin,
                            MessageBundle messageBundle,
                            JmixButton toggleButton,
                            SidePanelLayout sidePanel,
                            VerticalLayout content,
                            AiConversation conversation) {
        if (toggleButton == null) {
            return;
        }

        int count = contextAggregator.aggregate(conversation).totalCount();
        toggleButton.setText(messageBundle.getMessage("attachmentsToggleAction"));
        if (count > 0) {
            Span countBadge = new Span(String.valueOf(count));
            countBadge.addClassName("ai-conversation-context-toggle-count");
            toggleButton.setSuffixComponent(countBadge);
        } else {
            toggleButton.setSuffixComponent(null);
        }

        if (sidePanel != null && sidePanel.isSidePanelOpened()) {
            render(origin, messageBundle, content, sidePanel, conversation);
        }
    }

    void openCrmEntityDetail(View<?> origin, MessageBundle messageBundle, String entityReference) {
        if (entityReference == null) {
            return;
        }

        try {
            Id<Object> id = idSerialization.stringToId(entityReference);
            Object entity = dataManager.load(id).one();
            if (entity instanceof UuidEntity uuidEntity) {
                //noinspection unchecked,rawtypes
                dialogWindows.detail(origin, (Class) uuidEntity.getClass())
                        .editEntity(uuidEntity)
                        .open();
            }
        } catch (Exception e) {
            log.error("Failed to open CRM entity detail from timeline", e);
            notifications.create(messageBundle.getMessage("entityReferenceOpenError"))
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    void downloadAttachment(AiConversationAttachment attachment) {
        if (attachment == null || attachment.getFile() == null) {
            return;
        }
        downloader.setShowNewWindow(true);
        downloader.download(attachment.getFile());
    }
}
