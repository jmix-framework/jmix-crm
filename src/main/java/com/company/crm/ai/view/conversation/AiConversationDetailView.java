package com.company.crm.ai.view.conversation;

import com.company.crm.ai.view.conversation.composer.AiConversationComposerFragment;
import com.company.crm.ai.view.conversation.component.ConversationTitleEditDialog;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import com.company.crm.ai.view.conversation.task.PendingAssistantResponseSupport;
import com.company.crm.ai.view.conversation.support.AiConversationActorNameResolver;
import com.company.crm.ai.view.conversation.task.AssistantResponseTaskCoordinator;
import com.company.crm.ai.view.timeline.TimelineItem;
import com.company.crm.ai.view.timeline.AiTimelineComponentFactory;
import com.company.crm.ai.view.timeline.AiConversationTimelineItemFactory;
import com.company.crm.ai.view.context.ConversationContextPanelSupport;
import com.company.crm.ai.config.CrmAiConfig;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.sidepanellayout.SidePanelLayout;
import io.jmix.flowui.component.virtuallist.JmixVirtualList;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.InstanceLoader;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.PrimaryDetailView;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Route(value = "ai-conversations/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.AI_CONVERSATION_DETAIL)
@ViewDescriptor(path = "ai-conversation-detail-view.xml")
@EditedEntityContainer("aiConversationDc")
@PrimaryDetailView(AiConversation.class)
public class AiConversationDetailView extends StandardDetailView<AiConversation> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationDetailView.class);

    @ViewComponent
    private InstanceContainer<AiConversation> aiConversationDc;
    @ViewComponent
    private VerticalLayout timelineContentLayout;
    @ViewComponent
    private VerticalLayout composerContainer;
    @ViewComponent
    private JmixButton attachmentsToggleBtn;
    @ViewComponent
    private SidePanelLayout contextSidePanel;
    @ViewComponent
    private VerticalLayout contextSidePanelContent;
    @ViewComponent
    private MessageBundle messageBundle;
    @ViewComponent
    private JmixButton editConversationTitleBtn;

    @Autowired
    private AiConversationService aiConversationService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private AiConversationActorNameResolver aiConversationActorNameResolver;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private CrmAiConfig crmAiConfig;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Fragments fragments;
    @Autowired
    private AssistantResponseTaskCoordinator assistantResponseTaskCoordinator;
    @Autowired
    private ConversationContextPanelSupport contextPanelSupport;
    @Autowired
    private PendingAssistantResponseSupport pendingAssistantResponseSupport;

    private JmixVirtualList<TimelineItem> timelineList;
    private AiConversationComposerFragment composerFragment;
    private TimelineItem activeThinkingItem;
    private AiTimelineComponentFactory timelineComponentFactory;
    private ConversationTitleEditDialog titleEditDialog;
    private UUID freshAssistantMessageId;
    private final AiConversationTimelineItemFactory timelineItemFactory = new AiConversationTimelineItemFactory();
    private List<TimelineItem> timelineItems = List.of();

    @Subscribe
    public void onInit(final InitEvent event) {
        initDynamicComponentsIfNeeded();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        setShowSaveNotification(false);

        if (!crmAiConfig.isAiIntegrationEnabled()) {
            notifications.create(messageBundle.getMessage("errorInvalidApiKey"))
                    .withType(Notifications.Type.ERROR)
                    .withDuration(0)
                    .show();
            composerFragment.setInputEnabled(false);
        }

        refreshTimelineItems();
        refreshComposerState();
        refreshAttachmentsToggleLabel();
        focusInput();
        triggerPendingAssistantResponseIfNeeded();
    }

    @Subscribe(id = "aiConversationDl", target = Target.DATA_LOADER)
    public void onAiConversationDlPostLoad(final InstanceLoader.PostLoadEvent<AiConversation> event) {
        refreshTimelineItems();
        refreshComposerState();
        refreshAttachmentsToggleLabel();
    }

    @Subscribe("editConversationTitleBtn")
    public void onEditConversationTitleBtnClick(final ClickEvent<JmixButton> event) {
        titleEditDialog().open(this, aiConversationDc.getItem(), () -> {
                    getViewData().getDataContext().save();
                    reloadViewData();
                }
        );
    }

    @Subscribe("attachmentsToggleBtn")
    public void onAttachmentsToggleBtnClick(final ClickEvent<JmixButton> event) {
        renderContextSidePanel();
        contextSidePanel.openSidePanel();
    }

    private void submitUserMessageFromComposer(AiConversationComposerFragment.Submission submission) {
        submitUserMessage(submission.prompt(), submission.entityReferences(), submission.attachments());
    }

    private void submitUserMessage(String userMessage,
                                   List<String> entityReferences,
                                   List<PendingAttachmentInput> attachments) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }

        AiConversation conversation = aiConversationDc.getItem();

        ChatMessage savedUserMessage;
        try {
            savedUserMessage = aiConversationService.createUserMessageAndEnsureStarted(
                    conversation,
                    userMessage.trim(),
                    entityReferences,
                    attachments
            );
        } catch (Exception e) {
            log.error("Failed to persist user message with context", e);
            notifications.create(messageBundle.getMessage("errorProcessingMessage"))
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }

        appendTimelineItem(TimelineItem.user(savedUserMessage));
        composerFragment.clear();
        refreshComposerState();

        showThinkingIndicator();
        composerFragment.setInputEnabled(false);

        runAssistantResponseTask(savedUserMessage);
    }

    private void runAssistantResponseTask(ChatMessage savedUserMessage) {
        assistantResponseTaskCoordinator.run(
                this,
                aiConversationDc.getItem(),
                savedUserMessage,
                this::appendThinkingStatusUpdate,
                this::handleAssistantResponseDone,
                this::showAssistantProcessingError
        );
    }

    private void handleAssistantResponseDone(ChatMessage finalMessage) {
        // Tools can persist attachments in a separate transaction, so the view's
        // data container is stale here and needs a full reload.
        if (finalMessage != null) {
            freshAssistantMessageId = finalMessage.getId();
        }
        activeThinkingItem = null;
        getViewData().loadAll();
        focusInput();
    }

    private void showAssistantProcessingError() {
        removeThinkingIndicator();
        appendTimelineItem(TimelineItem.assistant(transientAssistantMessage(messageBundle.getMessage("errorProcessingMessage"))));
        focusInput();
    }

    private void showThinkingIndicator() {
        ChatMessage placeholder = transientAssistantMessage("");
        activeThinkingItem = TimelineItem.thinking(placeholder);
        appendTimelineItem(activeThinkingItem);
    }

    private void appendThinkingStatusUpdate(AiUiStatusUpdate statusUpdate) {
        if (activeThinkingItem == null || statusUpdate == null || statusUpdate.message() == null || statusUpdate.message().isBlank()) {
            return;
        }

        List<AiUiStatusUpdate> statusUpdates = activeThinkingItem.statusUpdates();
        if (!statusUpdates.isEmpty()) {
            AiUiStatusUpdate last = statusUpdates.get(statusUpdates.size() - 1);
            // Only fold into the last entry if it is still in-flight (no result yet).
            // A completed entry with the same base message belongs to a previous tool
            // call and must not swallow a fresh start phrase for the next call.
            if (last.message().equals(statusUpdate.message()) && !last.isCompleted()) {
                if (statusUpdate.isCompleted()) {
                    statusUpdates.set(statusUpdates.size() - 1, statusUpdate);
                    refreshTimelineItem(activeThinkingItem);
                    scrollToBottom();
                }
                return;
            }
        }

        statusUpdates.add(statusUpdate);
        if (statusUpdates.size() > 6) {
            statusUpdates.remove(0);
        }
        refreshTimelineItem(activeThinkingItem);
        scrollToBottom();
    }

    private void refreshTimelineItem(TimelineItem item) {
        timelineList.getDataProvider().refreshItem(item);
    }

    private void removeThinkingIndicator() {
        if (activeThinkingItem == null) {
            return;
        }
        freshAssistantMessageId = null;
        List<TimelineItem> updatedItems = new ArrayList<>(timelineItems);
        updatedItems.remove(activeThinkingItem);
        timelineItems = updatedItems;
        timelineList.setItems(timelineItems);
        activeThinkingItem = null;
    }

    public void reloadViewData() {
        // Title-edit and similar non-AI reloads should not re-animate a previously fresh
        // assistant row, so clear the marker before the rebuild.
        freshAssistantMessageId = null;
        getViewData().loadAll();
        refreshTimelineItems();
        refreshComposerState();
        refreshAttachmentsToggleLabel();
    }

    public void setOpenedInSideDialog(boolean openedInSideDialog) {
        if (openedInSideDialog) {
            getContent().addClassName("opened-in-side-dialog");
            contextSidePanel.setSidePanelPosition(io.jmix.flowui.kit.component.sidepanellayout.SidePanelPosition.BOTTOM);
            contextSidePanel.addClassName("ai-conversation-context-side-panel-dialog");
        }
    }

    private void refreshTimelineItems() {
        timelineItems = timelineItemFactory.buildTimelineItems(aiConversationDc.getItem());
        timelineList.setItems(timelineItems);
        scrollToBottom();
    }

    private void appendTimelineItem(TimelineItem item) {
        freshAssistantMessageId = null;
        List<TimelineItem> updatedItems = new ArrayList<>(timelineItems);
        updatedItems.add(item);
        timelineItems = updatedItems;
        timelineList.setItems(timelineItems);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!timelineItems.isEmpty()) {
            timelineList.scrollToIndex(timelineItems.size() - 1);
        }
    }

    private void refreshComposerState() {
        configureTimelineLayout();

        if (isReadOnly()) {
            editConversationTitleBtn.setVisible(false);
            composerContainer.setVisible(false);
            return;
        }

        editConversationTitleBtn.setVisible(true);
        composerContainer.setVisible(true);
        composerFragment.configureTimelineLayout();
    }

    private void configureTimelineLayout() {
        timelineList.getStyle().set("width", "100%");
        timelineList.setHeightFull();
        timelineContentLayout.setFlexGrow(1, timelineList);
        timelineContentLayout.setFlexGrow(0, composerContainer);
    }



    private Component createTimelineComponent(TimelineItem item) {
        return timelineComponentFactory.createTimelineComponent(item);
    }

    private ChatMessage transientAssistantMessage(String content) {
        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(aiConversationDc.getItem());
        message.setContent(content);
        message.setType(ChatMessageType.ASSISTANT);
        message.setCreatedDate(timeSource.now().toOffsetDateTime());
        message.setCreatedBy(currentAuthentication.getUser().getUsername());
        return message;
    }

    private void focusInput() {
        composerFragment.setEnabledAndFocus(crmAiConfig.isAiIntegrationEnabled());
    }

    @SuppressWarnings("unchecked")
    private void initDynamicComponentsIfNeeded() {
        ensureTimelineComponentFactory();
        ensureTimelineList();
        ensureComposerFragment();
        attachDynamicComponents();
    }

    private void ensureTimelineComponentFactory() {
        if (timelineComponentFactory == null) {
            timelineComponentFactory = new AiTimelineComponentFactory(
                    uiComponents,
                    messageBundle,
                    contextPanelSupport.contextCardFactory(this, messageBundle),
                    this::resolveActorName,
                    datatypeFormatter::formatOffsetDateTime,
                    () -> freshAssistantMessageId
            );
        }
    }

    private void ensureTimelineList() {
        if (timelineList == null) {
            timelineList = uiComponents.create(JmixVirtualList.class);
            timelineList.setWidthFull();
            timelineList.setRenderer(new ComponentRenderer<>(this::createTimelineComponent));
            timelineList.addClassName("ai-conversation-timeline-list");
        }
    }

    private void ensureComposerFragment() {
        if (composerFragment == null) {
            composerFragment = fragments.create(this, AiConversationComposerFragment.class);
            composerFragment.setVariant(AiConversationComposerFragment.Variant.TIMELINE);
            composerFragment.setSubmitHandler(this::submitUserMessageFromComposer);
            composerFragment.setPendingContextActions(
                    contextPanelSupport::downloadAttachment,
                    entityReference -> contextPanelSupport.openCrmEntityDetail(this, messageBundle, entityReference)
            );
        }
    }

    private void attachDynamicComponents() {
        if (timelineList.getParent().isEmpty()) {
            timelineContentLayout.addComponentAsFirst(timelineList);
        }
        if (composerFragment.getParent().isEmpty()) {
            composerContainer.addComponentAsFirst(composerFragment);
        }
    }

    private String resolveActorName(ChatMessage message) {
        return aiConversationActorNameResolver.resolve(message, messageBundle.getMessage("defaultActorName"));
    }

    private void renderContextSidePanel() {
        contextPanelSupport.render(this, messageBundle, contextSidePanelContent, contextSidePanel, aiConversationDc.getItem());
    }

    private void refreshAttachmentsToggleLabel() {
        contextPanelSupport.refreshToggleLabel(
                this,
                messageBundle,
                attachmentsToggleBtn,
                contextSidePanel,
                contextSidePanelContent,
                aiConversationDc.getItem()
        );
    }

    private ConversationTitleEditDialog titleEditDialog() {
        if (titleEditDialog == null) {
            titleEditDialog = new ConversationTitleEditDialog(dialogs, messageBundle);
        }
        return titleEditDialog;
    }



    private void triggerPendingAssistantResponseIfNeeded() {
        if (isReadOnly() || !crmAiConfig.isAiIntegrationEnabled() || activeThinkingItem != null) {
            return;
        }

        pendingAssistantResponseSupport.findPendingUserMessage(aiConversationDc.getItem())
                .ifPresent(userMessage -> {
                    showThinkingIndicator();
                    composerFragment.setInputEnabled(false);
                    runAssistantResponseTask(userMessage);
                });
    }

}
