package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.config.CrmAiConfig;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiPromptSuggestionDto;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationStartService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
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
import io.jmix.flowui.component.gridlayout.GridLayout;
import io.jmix.flowui.component.sidepanellayout.SidePanelLayout;
import io.jmix.flowui.component.virtuallist.JmixVirtualList;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.InstanceLoader;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.PrimaryDetailView;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;
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
    private CollectionContainer<AiPromptSuggestionDto> promptSuggestionsDc;
    @ViewComponent
    private VerticalLayout timelineContentLayout;
    @ViewComponent
    private VerticalLayout composerContainer;
    @ViewComponent
    private Component promptSuggestionsHeading;
    @ViewComponent
    private GridLayout<AiPromptSuggestionDto> promptSuggestionsGridLayout;
    @ViewComponent
    private Component topSpacer;
    @ViewComponent
    private Component bottomSpacer;
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
    private AiConversationStartService aiConversationStartService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
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
    private AiConversationTimelineRenderer timelineRenderer;
    private PromptSuggestionSupport promptSuggestionSupport;
    private ConversationTitleEditDialog titleEditDialog;
    private UUID freshAssistantMessageId;
    private final AiConversationTimelineItemFactory timelineItemFactory = new AiConversationTimelineItemFactory();
    private List<TimelineItem> timelineItems = List.of();
    private List<String> initialEntityReferences = List.of();
    private boolean initialEntityReferencesApplied;

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

        applyInitialEntityReferencesIfNeeded();
        refreshTimelineItems();
        refreshPromptSuggestions();
        refreshComposerState();
        refreshAttachmentsToggleLabel();
        focusInput();
        triggerPendingAssistantResponseIfNeeded();
    }

    @Subscribe(id = "aiConversationDl", target = Target.DATA_LOADER)
    public void onAiConversationDlPostLoad(final InstanceLoader.PostLoadEvent<AiConversation> event) {
        applyInitialEntityReferencesIfNeeded();
        refreshTimelineItems();
        refreshPromptSuggestions();
        refreshComposerState();
        refreshAttachmentsToggleLabel();
    }

    public void setInitialEntityReferences(Collection<String> entityReferences) {
        this.initialEntityReferences = entityReferences != null ? List.copyOf(entityReferences) : List.of();
        this.initialEntityReferencesApplied = false;
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

    private void submitUserMessage(String userMessage) {
        submitUserMessage(userMessage, composerFragment.entityReferences(), composerFragment.attachments());
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
            savedUserMessage = aiConversationStartService.createUserMessageAndEnsureStarted(
                    conversation,
                    userMessage.trim(),
                    entityReferences,
                    attachments
            );
            refreshPromptSuggestions();
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
        promptSuggestionsGridLayout.setEnabled(false);

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
        appendTimelineItem(TimelineItem.assistant(transientMessage(
                messageBundle.getMessage("errorProcessingMessage"),
                ChatMessageType.ASSISTANT
        )));
        focusInput();
    }

    private void showThinkingIndicator() {
        ChatMessage placeholder = transientMessage("", ChatMessageType.ASSISTANT);
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
        if (timelineList != null && timelineList.getDataProvider() != null) {
            timelineList.getDataProvider().refreshItem(item);
        }
    }

    private void removeThinkingIndicator() {
        if (activeThinkingItem == null) {
            return;
        }
        freshAssistantMessageId = null;
        List<TimelineItem> updatedItems = new ArrayList<>(timelineItems);
        updatedItems.remove(activeThinkingItem);
        timelineItems = updatedItems;
        if (timelineList != null) {
            timelineList.setItems(timelineItems);
        }
        activeThinkingItem = null;
    }

    private void reloadViewData() {
        // Title-edit and similar non-AI reloads should not re-animate a previously fresh
        // assistant row, so clear the marker before the rebuild.
        freshAssistantMessageId = null;
        getViewData().loadAll();
        refreshTimelineItems();
        refreshPromptSuggestions();
        refreshComposerState();
        refreshAttachmentsToggleLabel();
    }

    private void refreshTimelineItems() {
        if (timelineList == null) {
            return;
        }
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
        if (timelineList != null && !timelineItems.isEmpty()) {
            timelineList.scrollToIndex(timelineItems.size() - 1);
        }
    }

    private void refreshPromptSuggestions() {
        AiConversation conversation = aiConversationDc.getItem();

        if (Boolean.TRUE.equals(conversation.getFirstMessageSent()) || hasInitialEntityReferences()) {
            promptSuggestionsDc.setItems(List.of());
            promptSuggestionsHeading.setVisible(false);
            promptSuggestionsGridLayout.setVisible(false);
            return;
        }

        if (promptSuggestionsDc.getItems().isEmpty()) {
            promptSuggestionsDc.setItems(promptSuggestionSupport().selectInitialSuggestions());
        }

        boolean hasSuggestions = !promptSuggestionsDc.getItems().isEmpty();
        promptSuggestionsHeading.setVisible(hasSuggestions);
        promptSuggestionsGridLayout.setVisible(hasSuggestions);
        promptSuggestionsGridLayout.setEnabled(crmAiConfig.isAiIntegrationEnabled());
    }

    private void refreshComposerState() {
        if (isReadOnly()) {
            if (editConversationTitleBtn != null) {
                editConversationTitleBtn.setVisible(false);
            }
            composerContainer.setVisible(false);
            topSpacer.setVisible(false);
            bottomSpacer.setVisible(false);
            promptSuggestionsHeading.setVisible(false);
            promptSuggestionsGridLayout.setVisible(false);

            if (timelineContentLayout != null) {
                timelineContentLayout.setFlexGrow(1, timelineList);
                timelineContentLayout.setFlexGrow(0, topSpacer);
                timelineContentLayout.setFlexGrow(0, composerContainer);
                timelineContentLayout.setFlexGrow(0, bottomSpacer);
            }

            if (timelineList != null) {
                timelineList.setHeightFull();
            }

            refreshContentWidth();
            return;
        }

        boolean conversationStarted = isConversationStarted();
        boolean hasPromptSuggestions = !promptSuggestionsDc.getItems().isEmpty();

        topSpacer.setVisible(!conversationStarted);
        bottomSpacer.setVisible(!conversationStarted);
        promptSuggestionsHeading.setVisible(!conversationStarted && hasPromptSuggestions);
        promptSuggestionsGridLayout.setVisible(!conversationStarted && hasPromptSuggestions);

        if (timelineContentLayout != null) {
            timelineContentLayout.setFlexGrow(conversationStarted ? 1 : 0, timelineList);
            timelineContentLayout.setFlexGrow(conversationStarted ? 0 : 0.75, topSpacer);
            timelineContentLayout.setFlexGrow(0, composerContainer);
            timelineContentLayout.setFlexGrow(conversationStarted ? 0 : 1.25, bottomSpacer);
        }

        if (composerFragment != null) {
            var inputBar = composerFragment.getInputBar();
            var messageInput = composerFragment.getMessageInput();
            if (inputBar != null && messageInput != null) {
                inputBar.setFlexGrow(1, messageInput);
            }
        }

        if (timelineList != null) {
            timelineList.setHeightFull();
            if (!conversationStarted) {
                timelineList.setHeight(null);
            }
        }

        refreshContentWidth();
    }

    private void refreshContentWidth() {
        if (timelineList == null || composerFragment == null) {
            return;
        }

        var messageInput = composerFragment.getMessageInput();
        var pendingContextLayout = composerFragment.getPendingContextLayout();
        var inputBar = composerFragment.getInputBar();
        var attachmentUpload = composerFragment.getAttachmentUpload();
        var addMenuBar = composerFragment.getAddMenuBar();

        if (messageInput == null || pendingContextLayout == null || inputBar == null 
                || attachmentUpload == null || addMenuBar == null) {
            return;
        }

        timelineList.getStyle().set("width", "100%");
        pendingContextLayout.getStyle().set("width", "100%");
        inputBar.getStyle().set("width", "100%");

        attachmentUpload.getStyle().set("width", "auto");
        addMenuBar.getStyle().set("flex-shrink", "0");
    }

    private void applyInitialEntityReferencesIfNeeded() {
        if (initialEntityReferencesApplied || initialEntityReferences.isEmpty()) {
            return;
        }

        composerFragment.addEntityReferences(initialEntityReferences);
        initialEntityReferencesApplied = true;
    }

    private boolean hasInitialEntityReferences() {
        return !initialEntityReferences.isEmpty() || initialEntityReferencesApplied;
    }

    private Component createTimelineComponent(TimelineItem item) {
        return timelineRenderer.createTimelineComponent(item);
    }

    private ChatMessage transientMessage(String content, ChatMessageType type) {
        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(aiConversationDc.getItem());
        message.setContent(content);
        message.setType(type);
        message.setCreatedDate(timeSource.now().toOffsetDateTime());
        message.setCreatedBy(currentAuthentication.getUser().getUsername());
        return message;
    }

    private void focusInput() {
        boolean aiEnabled = crmAiConfig.isAiIntegrationEnabled();
        composerFragment.setInputEnabled(aiEnabled);
        promptSuggestionsGridLayout.setEnabled(aiEnabled);
        if (aiEnabled) {
            composerFragment.focus();
        }
    }

    @SuppressWarnings("unchecked")
    private void initDynamicComponentsIfNeeded() {
        initTimelineRenderingIfNeeded();
        if (timelineList == null) {
            timelineList = uiComponents.create(JmixVirtualList.class);
            timelineList.setWidthFull();
            timelineList.setRenderer(new ComponentRenderer<>(this::createTimelineComponent));
            timelineList.addClassName("ai-conversation-timeline-list");
        }
        if (composerFragment == null) {
            composerFragment = fragments.create(this, AiConversationComposerFragment.class);
            composerFragment.setVariant(AiConversationComposerFragment.Variant.TIMELINE);
            composerFragment.setSubmitHandler(this::submitUserMessageFromComposer);
            composerFragment.setPendingContextActions(
                    contextPanelSupport::downloadAttachment,
                    entityReference -> contextPanelSupport.openCrmEntityDetail(this, messageBundle, entityReference)
            );
        }

        if (timelineContentLayout != null && timelineList.getParent().isEmpty()) {
            timelineContentLayout.addComponentAsFirst(timelineList);
        }
        if (composerContainer != null && composerFragment.getParent().isEmpty()) {
            composerContainer.addComponentAsFirst(composerFragment);
        }
    }

    private void initTimelineRenderingIfNeeded() {
        if (timelineRenderer == null) {
            timelineRenderer = new AiConversationTimelineRenderer(
                    messageBundle,
                    contextPanelSupport.contextCardFactory(this, messageBundle),
                    this::resolveActorName,
                    this::formatMessageTime,
                    () -> freshAssistantMessageId
            );
        }
    }

    private String resolveActorName(ChatMessage message) {
        String createdBy = message.getCreatedBy();
        UserDetails currentUser = currentAuthentication.getUser();
        if (createdBy == null || createdBy.isBlank() || createdBy.equals(currentUser.getUsername())) {
            if (currentUser instanceof com.company.crm.model.user.User crmUser) {
                String fullName = crmUser.getFullName();
                if (fullName != null && !fullName.isBlank()) {
                    return fullName;
                }
            }
            return currentUser.getUsername() != null && !currentUser.getUsername().isBlank()
                    ? currentUser.getUsername()
                    : messageBundle.getMessage("defaultActorName");
        }
        return createdBy;
    }

    private String formatMessageTime(OffsetDateTime createdDate) {
        if (createdDate == null) {
            return "";
        }
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(currentAuthentication.getLocale())
                .format(createdDate);
    }

    private PromptSuggestionSupport promptSuggestionSupport() {
        if (promptSuggestionSupport == null) {
            promptSuggestionSupport = new PromptSuggestionSupport(
                    uiComponents,
                    dataManager,
                    messageBundle,
                    this::submitUserMessage
            );
        }
        return promptSuggestionSupport;
    }

    private ConversationTitleEditDialog titleEditDialog() {
        if (titleEditDialog == null) {
            titleEditDialog = new ConversationTitleEditDialog(dialogs, messageBundle);
        }
        return titleEditDialog;
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

    @Supply(to = "promptSuggestionsGridLayout", subject = "renderer")
    private ComponentRenderer<Card, AiPromptSuggestionDto> promptSuggestionsGridLayoutRenderer() {
        return new ComponentRenderer<>(suggestion -> promptSuggestionSupport().createPromptSuggestionCard(suggestion));
    }

    private boolean isConversationStarted() {
        return Boolean.TRUE.equals(aiConversationDc.getItem().getFirstMessageSent());
    }

    private void triggerPendingAssistantResponseIfNeeded() {
        if (isReadOnly() || !crmAiConfig.isAiIntegrationEnabled() || activeThinkingItem != null) {
            return;
        }

        pendingAssistantResponseSupport.findPendingUserMessage(aiConversationDc.getItem())
                .ifPresent(userMessage -> {
                    showThinkingIndicator();
                    composerFragment.setInputEnabled(false);
                    promptSuggestionsGridLayout.setEnabled(false);
                    runAssistantResponseTask(userMessage);
                });
    }

}
