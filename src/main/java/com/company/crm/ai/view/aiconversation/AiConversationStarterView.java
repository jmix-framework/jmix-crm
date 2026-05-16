package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.config.CrmAiConfig;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiPromptSuggestionDto;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationStartService;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.SupportsTypedValue;
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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private CollectionContainer<AiPromptSuggestionDto> promptSuggestionsDc;
    @ViewComponent
    private CollectionContainer<AiConversation> recentConversationsDc;
    @ViewComponent
    private CollectionContainer<AiConversation> historyConversationsDc;
    @ViewComponent
    private GridLayout<AiPromptSuggestionDto> promptSuggestionsGridLayout;
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
    private MessageBundle messageBundle;

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private AiConversationStartService aiConversationStartService;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private Notifications notifications;
    @Autowired
    private CrmAiConfig crmAiConfig;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private Fragments fragments;

    private AiConversationComposerFragment composerFragment;
    private PromptSuggestionSupport promptSuggestionSupport;
    private String historyFilter = "";

    @Subscribe
    public void onInit(final InitEvent event) {
        initComposer();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        if (!crmAiConfig.isAiIntegrationEnabled()) {
            notifications.create(messageBundle.getMessage("errorInvalidApiKey"))
                    .withType(Notifications.Type.ERROR)
                    .withDuration(0)
                    .show();
            composerFragment.setInputEnabled(false);
            promptSuggestionsGridLayout.setEnabled(false);
        }

        if (promptSuggestionsDc.getItems().isEmpty()) {
            promptSuggestionsDc.setItems(promptSuggestionSupport().selectInitialSuggestions());
        }
        refreshRecentConversationsVisibility();
        renderHistoryList();
        focusMessageInput();
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
            conversation = aiConversationStartService.startConversation(
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

        viewNavigators.detailView(this, AiConversation.class)
                .editEntity(conversation)
                .navigate();
    }

    @Supply(to = "promptSuggestionsGridLayout", subject = "renderer")
    private ComponentRenderer<Card, AiPromptSuggestionDto> promptSuggestionsGridLayoutRenderer() {
        return new ComponentRenderer<>(suggestion ->
                promptSuggestionSupport().createPromptSuggestionCard(suggestion));
    }

    @Supply(to = "recentConversationsGridLayout", subject = "renderer")
    private ComponentRenderer<Card, AiConversation> recentConversationsGridLayoutRenderer() {
        return new ComponentRenderer<>(this::createRecentConversationCard);
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

    private Card createRecentConversationCard(AiConversation conversation) {
        Card card = uiComponents.create(Card.class);
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_OUTLINED);
        card.getStyle().set("cursor", "pointer");
        card.getStyle().set("overflow", "hidden");
        card.getElement().addEventListener("click", event -> openConversation(conversation));

        Icon icon = CrmIcons.SPARKLES.create();
        icon.getStyle().set("flex-shrink", "0");
        card.setHeaderPrefix(icon);

        Span titleText = uiComponents.create(Span.class);
        titleText.setText(conversation.getInstanceName());
        titleText.addClassNames("font-semibold", "text-m");
        titleText.getStyle().set("display", "block");
        titleText.getStyle().set("overflow", "hidden");
        titleText.getStyle().set("text-overflow", "ellipsis");
        titleText.getStyle().set("white-space", "nowrap");
        titleText.getElement().setProperty("title", conversation.getInstanceName());
        card.setTitle(titleText);

        OffsetDateTime createdDate = conversation.getCreatedDate();
        if (createdDate != null) {
            Span subtitle = uiComponents.create(Span.class);
            subtitle.setText(formatDateTime(createdDate));
            subtitle.addClassNames("text-s", "text-secondary");
            card.setSubtitle(subtitle);
        }

        return card;
    }

    private void renderHistoryList() {
        if (historyListContainer == null) {
            return;
        }

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

        Map<String, List<AiConversation>> result = new LinkedHashMap<>();
        for (AiConversation conversation : conversations) {
            String bucket = bucketLabel(conversation, zone, today, yesterday, weekStart);
            result.computeIfAbsent(bucket, k -> new java.util.ArrayList<>()).add(conversation);
        }
        return result;
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
        VerticalLayout group = uiComponents.create(VerticalLayout.class);
        group.setPadding(false);
        group.setSpacing(false);
        group.setWidthFull();
        group.addClassName("ai-conversation-starter-history-group");

        HorizontalLayout groupHeader = uiComponents.create(HorizontalLayout.class);
        groupHeader.setWidthFull();
        groupHeader.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        groupHeader.addClassName("ai-conversation-starter-history-group-header");

        Span bucket = uiComponents.create(Span.class);
        bucket.setText(bucketLabel.toUpperCase(Locale.ROOT));
        bucket.addClassNames("text-secondary", "text-xs", "font-semibold");
        bucket.getStyle().set("letter-spacing", "0.04em");

        Span groupCount = uiComponents.create(Span.class);
        groupCount.setText(String.valueOf(conversations.size()));
        groupCount.addClassNames("text-secondary", "text-xs");
        groupCount.getStyle().set("margin-left", "var(--lumo-space-s)");

        groupHeader.add(bucket, groupCount);
        group.add(groupHeader);

        for (AiConversation conversation : conversations) {
            group.add(createHistoryItem(conversation));
        }
        return group;
    }

    private Component createHistoryItem(AiConversation conversation) {
        VerticalLayout item = uiComponents.create(VerticalLayout.class);
        item.setPadding(false);
        item.setSpacing(false);
        item.setWidthFull();
        item.addClassName("ai-conversation-starter-history-item");
        item.getStyle().set("cursor", "pointer");
        item.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-xs)");
        item.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        item.getElement().addEventListener("click", e -> openConversation(conversation));

        Span title = uiComponents.create(Span.class);
        title.setText(conversation.getInstanceName());
        title.addClassNames("font-semibold", "text-s");
        title.getStyle().set("display", "block");
        title.getStyle().set("overflow", "hidden");
        title.getStyle().set("text-overflow", "ellipsis");
        title.getStyle().set("white-space", "nowrap");
        item.add(title);

        String snippet = firstUserMessageSnippet(conversation);
        if (!snippet.isBlank()) {
            Span snippetSpan = uiComponents.create(Span.class);
            snippetSpan.setText(snippet);
            snippetSpan.addClassNames("text-secondary", "text-xs");
            snippetSpan.getStyle().set("display", "-webkit-box");
            snippetSpan.getStyle().set("-webkit-line-clamp", "2");
            snippetSpan.getStyle().set("-webkit-box-orient", "vertical");
            snippetSpan.getStyle().set("overflow", "hidden");
            snippetSpan.getStyle().set("line-height", "1.3");
            item.add(snippetSpan);
        }

        OffsetDateTime created = conversation.getCreatedDate();
        if (created != null) {
            Span meta = uiComponents.create(Span.class);
            meta.setText(formatDateTime(created));
            meta.addClassNames("text-tertiary", "text-xs");
            meta.getStyle().set("margin-top", "var(--lumo-space-xs)");
            item.add(meta);
        }

        Div separator = uiComponents.create(Div.class);
        separator.getStyle().set("height", "1px");
        separator.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        separator.getStyle().set("margin-top", "var(--lumo-space-s)");
        item.add(separator);

        return item;
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
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(currentAuthentication.getLocale())
                .format(dateTime);
    }

    private void openConversation(AiConversation conversation) {
        viewNavigators.detailView(this, AiConversation.class)
                .editEntity(conversation)
                .navigate();
    }

    private PromptSuggestionSupport promptSuggestionSupport() {
        if (promptSuggestionSupport == null) {
            promptSuggestionSupport = new PromptSuggestionSupport(
                    uiComponents,
                    dataManager,
                    messageBundle,
                    this::startConversationWithPrompt
            );
        }
        return promptSuggestionSupport;
    }
}
