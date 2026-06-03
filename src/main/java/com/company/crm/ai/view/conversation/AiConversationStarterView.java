package com.company.crm.ai.view.conversation;

import com.company.crm.ai.view.conversation.composer.AiConversationComposerFragment;
import com.company.crm.ai.view.conversation.component.AiConversationCard;
import com.company.crm.ai.view.conversation.component.AiConversationHistoryGroup;
import com.company.crm.ai.view.conversation.component.AiSideDialogHeader;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import com.company.crm.ai.view.component.support.PromptSuggestionSupport;
import com.company.crm.ai.config.CrmAiConfig;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiPromptSuggestion;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.app.icons.CrmIcons;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Views;
import io.jmix.flowui.component.sidedialog.SideDialog;
import io.jmix.flowui.kit.component.sidedialog.SideDialogPosition;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.SupportsTypedValue;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.component.gridlayout.GridLayout;
import io.jmix.flowui.component.sidepanellayout.SidePanelLayout;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "ai-conversations", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.AI_CONVERSATION_STARTER)
@ViewDescriptor(path = "ai-conversation-starter-view.xml")
public class AiConversationStarterView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(AiConversationStarterView.class);

    @ViewComponent
    private VerticalLayout composerCard;
    @ViewComponent
    private Div starterHeroIcon;
    @ViewComponent
    private CollectionContainer<AiPromptSuggestion> promptSuggestionsDc;
    @ViewComponent
    private CollectionContainer<AiConversation> recentConversationsDc;
    @ViewComponent
    private CollectionContainer<AiConversation> historyConversationsDc;
    @ViewComponent
    private GridLayout<AiPromptSuggestion> promptSuggestionsGridLayout;
    @ViewComponent
    private GridLayout<AiConversation> recentConversationsGridLayout;
    @ViewComponent
    private Component recentConversationsHeader;
    @ViewComponent
    private SidePanelLayout historySidePanel;
    @ViewComponent
    private VerticalLayout historyListContainer;
    @ViewComponent
    private Span historyPanelCount;
    @ViewComponent
    private TypedTextField<String> historySearchField;
    @ViewComponent
    private JmixButton showAllHistoryBtn;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private AiConversationService aiConversationService;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private Views views;
    @Autowired
    private Notifications notifications;
    @Autowired
    private CrmAiConfig crmAiConfig;
    @Autowired
    private Fragments fragments;
    @Autowired
    private IdSerialization idSerialization;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    private AiConversationComposerFragment composerFragment;
    private PromptSuggestionSupport promptSuggestionSupport;
    private String historyFilter = "";
    private boolean openedInDialog = false;
    private SideDialog parentSideDialog;
    private List<String> initialEntityReferences = List.of();
    private boolean initialEntityReferencesApplied = false;
    private boolean starterViewActivated = false;

    public void setParentSideDialog(SideDialog parentSideDialog) {
        this.parentSideDialog = parentSideDialog;
    }

    public void setOpenedInDialog(boolean openedInDialog) {
        this.openedInDialog = openedInDialog;
        if (openedInDialog) {
            getContent().addClassName("opened-in-dialog");
        }
    }

    public void setInitialEntityReferences(Collection<String> entityReferences) {
        this.initialEntityReferences = entityReferences != null ? List.copyOf(entityReferences) : List.of();
        this.initialEntityReferencesApplied = false;
        this.starterViewActivated = false;
    }

    private void applyInitialEntityReferencesIfNeeded() {
        if (initialEntityReferencesApplied || initialEntityReferences.isEmpty()) {
            return;
        }

        composerFragment.addEntityReferences(initialEntityReferences);
        initialEntityReferencesApplied = true;
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        initComposer();
        promptSuggestionSupport = new PromptSuggestionSupport(
                uiComponents,
                dataManager,
                idSerialization,
                messageBundle,
                this::startConversationWithPrompt
        );
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        activateStarterView();
    }

    public void activateStarterView() {
        if (starterViewActivated) {
            return;
        }
        starterViewActivated = true;

        if (openedInDialog) {
            showAllHistoryBtn.setVisible(false);
        }

        if (!crmAiConfig.isAiIntegrationEnabled()) {
            notifications.create(messageBundle.getMessage("errorInvalidApiKey"))
                    .withType(Notifications.Type.ERROR)
                    .withDuration(0)
                    .show();
            composerFragment.setInputEnabled(false);
            promptSuggestionsGridLayout.setEnabled(false);
        }

        applyInitialEntityReferencesIfNeeded();
        if (promptSuggestionsDc.getItems().isEmpty()) {
            promptSuggestionsDc.setItems(promptSuggestionSupport
                    .selectSuggestionsForEntityReferences(initialEntityReferences));
            showPromptSuggestionsGrid();
        }
        refreshRecentConversationsVisibility();

        if (!openedInDialog) {
            renderHistoryList();
        }

        focusMessageInput();

        if (parentSideDialog != null) {
            historySidePanel.setSidePanelHorizontalSize("100%");
            historySidePanel.setSidePanelHorizontalMinSize("100%");
            historySidePanel.setSidePanelHorizontalMaxSize("100%");
        }
    }

    /**
     * Plain MessageInput focus sometimes loses the race against the menu bar's tab-index
     * when the view first mounts, so the composer performs the deferred focus call.
     */
    private void focusMessageInput() {
        composerFragment.focus();
    }

    private void initComposer() {
        composerFragment = fragments.create(this, AiConversationComposerFragment.class);
        composerFragment.setVariant(AiConversationComposerFragment.Variant.STARTER);
        composerFragment.setSubmitHandler(this::startConversationFromComposer);

        composerCard.removeAll();
        composerCard.add(composerFragment);

        Icon heroIcon = CrmIcons.SPARKLES.create();
        heroIcon.addClassName("ai-conversation-starter-hero-icon-glyph");
        starterHeroIcon.removeAll();
        starterHeroIcon.add(heroIcon);
    }

    private void refreshRecentConversationsVisibility() {
        boolean hasRecent = !recentConversationsDc.getItems().isEmpty();
        recentConversationsHeader.setVisible(hasRecent);
        recentConversationsGridLayout.setVisible(hasRecent);
    }

    private void startConversationFromComposer(AiConversationComposerFragment.Submission submission) {
        startConversationWithPrompt(submission.prompt(), submission.entityReferences(), submission.attachments());
    }

    private void startConversationWithPrompt(String prompt) {
        startConversationWithPrompt(prompt, composerFragment.entityReferences(), composerFragment.attachments());
    }

    private void startConversationWithPrompt(String prompt,
                                             List<String> entityReferences,
                                             List<PendingAttachmentInput> attachments) {
        if (prompt == null || prompt.isBlank()) {
            return;
        }

        AiConversation conversation;
        try {
            conversation = aiConversationService.startConversation(
                    prompt.trim(),
                    entityReferences,
                    attachments
            ).conversation();
        } catch (Exception e) {
            log.error("Failed to start conversation from starter view", e);
            notifications.create(messageBundle.getMessage("errorProcessingMessage"))
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }

        composerFragment.clear();
        openConversationDetail(conversation);
    }

    private void showPromptSuggestionsGrid() {
        promptSuggestionsGridLayout.setVisible(true);
    }

    @Supply(to = "promptSuggestionsGridLayout", subject = "renderer")
    private ComponentRenderer<Card, AiPromptSuggestion> promptSuggestionsGridLayoutRenderer() {
        return new ComponentRenderer<>(suggestion ->
                promptSuggestionSupport.createPromptSuggestionCard(suggestion));
    }

    @Supply(to = "recentConversationsGridLayout", subject = "renderer")
    private ComponentRenderer<Card, AiConversation> recentConversationsGridLayoutRenderer() {
        return new ComponentRenderer<>(this::createConversationCard);
    }

    @Subscribe("showAllHistoryBtn")
    public void onShowAllHistoryBtnClick(final ClickEvent<JmixButton> event) {
        renderHistoryList();
        historySidePanel.openSidePanel();
    }

    @Subscribe("historyCloseBtn")
    public void onHistoryCloseBtnClick(final ClickEvent<JmixButton> event) {
        historySidePanel.closeSidePanel();
    }

    @Subscribe("historyNewBtn")
    public void onHistoryNewBtnClick(final ClickEvent<JmixButton> event) {
        historySidePanel.closeSidePanel();
        focusMessageInput();
    }

    @Subscribe("historySearchField")
    public void onHistorySearchFieldValueChange(
            final SupportsTypedValue.TypedValueChangeEvent<TypedTextField<String>, String> event) {
        historyFilter = Optional.ofNullable(event.getValue()).orElse("").trim().toLowerCase(Locale.ROOT);
        renderHistoryList();
    }

    private Card createConversationCard(AiConversation conversation) {
        AiConversationCard card = uiComponents.create(AiConversationCard.class);
        card.setConversation(conversation, this::openConversation, this::formatDateTime);
        return card;
    }

    private void renderHistoryList() {

        List<AiConversation> all = historyConversationsDc.getItems();
        historyPanelCount.setText(String.valueOf(all.size()));

        List<AiConversation> filtered = applyHistoryFilter(all);

        historyListContainer.removeAll();
        if (filtered.isEmpty()) {
            Span emptyState = uiComponents.create(Span.class);
            emptyState.setText(messageBundle.getMessage("aiConversationStarterView.historyEmpty"));
            emptyState.addClassNames("text-secondary", "text-s",
                    "ai-conversation-starter-history-empty");
            historyListContainer.add(emptyState);
            return;
        }

        Map<String, List<AiConversation>> grouped = groupByBucket(filtered);
        grouped.forEach((bucketLabel, items) -> historyListContainer.add(createHistoryGroup(bucketLabel, items)));
    }

    private List<AiConversation> applyHistoryFilter(List<AiConversation> conversations) {
        if (historyFilter.isEmpty()) {
            return conversations;
        }
        return conversations.stream()
                .filter(c -> {
                    String title = Optional.ofNullable(c.getTitle()).orElse("").toLowerCase(Locale.ROOT);
                    if (title.contains(historyFilter)) {
                        return true;
                    }
                    String snippet = firstUserMessageSnippet(c).toLowerCase(Locale.ROOT);
                    return snippet.contains(historyFilter);
                })
                .toList();
    }

    private Map<String, List<AiConversation>> groupByBucket(List<AiConversation> conversations) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.minusDays(7);

        return conversations.stream()
                .collect(Collectors.groupingBy(
                        conversation -> bucketLabel(conversation, zone, today, yesterday, weekStart),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private String bucketLabel(AiConversation conversation, ZoneId zone,
                               LocalDate today, LocalDate yesterday, LocalDate weekStart) {
        OffsetDateTime created = conversation.getCreatedDate();
        if (created == null) {
            return messageBundle.getMessage("aiConversationStarterView.historyBucketEarlier");
        }
        LocalDate date = created.atZoneSameInstant(zone).toLocalDate();
        if (!date.isBefore(today)) {
            return messageBundle.getMessage("aiConversationStarterView.historyBucketToday");
        }
        if (date.equals(yesterday)) {
            return messageBundle.getMessage("aiConversationStarterView.historyBucketYesterday");
        }
        if (date.isAfter(weekStart)) {
            return messageBundle.getMessage("aiConversationStarterView.historyBucketLastWeek");
        }
        return messageBundle.getMessage("aiConversationStarterView.historyBucketEarlier");
    }

    private Component createHistoryGroup(String bucketLabel, List<AiConversation> conversations) {
        AiConversationHistoryGroup group = uiComponents.create(AiConversationHistoryGroup.class);
        group.setGroup(bucketLabel, conversations, this::createConversationCard);
        return group;
    }

    private String firstUserMessageSnippet(AiConversation conversation) {
        List<ChatMessage> messages = conversation.getMessages();
        if (messages == null) {
            return "";
        }
        return messages.stream()
                .filter(Objects::nonNull)
                .filter(m -> ChatMessageType.USER.equals(m.getType()))
                .map(ChatMessage::getContent)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return datatypeFormatter.formatOffsetDateTime(dateTime);
    }

    private void openConversation(AiConversation conversation) {
        openConversationDetail(conversation);
    }

    private void openConversationDetail(AiConversation conversation) {
        if (parentSideDialog != null) {
            parentSideDialog.close();
            openDetailInSideDialog(conversation);
        } else if (openedInDialog) {
            close(StandardOutcome.CLOSE);
            DialogWindow<AiConversationDetailView> detailDialog = dialogWindows.detail(this, AiConversation.class)
                    .editEntity(conversation)
                    .withViewClass(AiConversationDetailView.class)
                    .build();

            detailDialog.setModal(false);
            detailDialog.setLeft("65%");
            detailDialog.setResizable(true);
            detailDialog.setTop("5%");
            detailDialog.setWidth("35%");
            detailDialog.setHeight("75%");
            detailDialog.open();
        } else {
            viewNavigators.detailView(this, AiConversation.class)
                    .editEntity(conversation)
                    .navigate();
        }
    }

    private void openDetailInSideDialog(AiConversation conversation) {
        AiConversationDetailView detailView = views.create(AiConversationDetailView.class);
        detailView.setEntityToEdit(conversation);
        detailView.setOpenedInSideDialog(true);
        detailView.reloadViewData();
        // TODO: bad way, refactor AiConversationDetailView
        detailView.addAttachListener(event -> {
            // ReadyEvent is not invoked, because view is not correctly opened.
            detailView.onReady(new ReadyEvent(detailView));
            event.unregisterListener();
        });

        SideDialog detailSideDialog = dialogs.createSideDialog()
                .withSideDialogPosition(SideDialogPosition.RIGHT)
                .withHorizontalSize("35%")
                .withModal(false)
                .withContentComponents(detailView)
                .withHeaderProvider(sd -> {
                    AiSideDialogHeader header = uiComponents.create(AiSideDialogHeader.class);
                    header.setDialog(sd);
                    return header;
                })
                .build();

        detailSideDialog.open();
    }

}
