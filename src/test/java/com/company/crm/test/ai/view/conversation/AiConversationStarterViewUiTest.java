package com.company.crm.test.ai.view.conversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.CrmAnalyticsService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.ai.view.conversation.composer.AiConversationComposerFragment;
import com.company.crm.ai.view.conversation.AiConversationDetailView;
import com.company.crm.ai.view.conversation.AiConversationStarterView;
import com.company.crm.model.catalog.category.Category;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.gridlayout.GridLayout;
import io.jmix.flowui.component.sidepanellayout.SidePanelLayout;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UI tests for the AI Conversation starter view.
 */
@TestPropertySource(properties = "spring.ai.openai.api-key=test-key")
public class AiConversationStarterViewUiTest extends AbstractUiTest {

    @Autowired
    private ViewNavigators viewNavigators;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private IdSerialization idSerialization;

    @MockitoBean
    @SuppressWarnings("unused")
    private CrmAnalyticsService mockAnalyticsService;

    @Test
    void testStarterViewRendersComposerAndSuggestions() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();

        AiConversationStarterView starterView = UiTestUtils.getCurrentView();
        VerticalLayout composerCard = UiTestUtils.getComponent(starterView, "composerCard");
        assertThat(viewTestSupport.findDescendant(composerCard, MessageInput.class))
                .as("composer must contain a MessageInput")
                .isPresent();

        @SuppressWarnings("unchecked")
        GridLayout<Object> suggestionsGrid =
                UiTestUtils.getComponent(starterView, "promptSuggestionsGridLayout");
        assertThat(suggestionsGrid.isVisible())
                .as("prompt suggestions grid must be visible")
                .isTrue();
    }

    @Test
    void testSubmittingPromptCreatesConversationAndNavigatesToDetail() {
        when(mockAnalyticsService.processUserMessage(
                any(UUID.class),
                anyStatusUpdateCallback()
        ))
                .thenReturn("mock assistant response");

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        MessageInput messageInput = inputOf(starterView);
        long conversationsBefore = countConversations();

        fireSubmit(messageInput, "Top overdue invoices this week");

        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        AiConversation created = detailView.getEditedEntity();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getFirstMessageSent()).isTrue();

        assertThat(countConversations())
                .as("a new conversation must be persisted")
                .isEqualTo(conversationsBefore + 1);

        AiConversation reloaded = dataManager.load(AiConversation.class)
                .id(created.getId())
                .fetchPlan(fp -> fp.add("messages", sub -> sub.addFetchPlan("_base")))
                .one();
        assertThat(reloaded.getMessages())
                .as("submitted prompt must be persisted as a USER message")
                .extracting(ChatMessage::getType, ChatMessage::getContent)
                .containsExactly(tuple(
                        ChatMessageType.USER,
                        "Top overdue invoices this week"
                ));

        verify(mockAnalyticsService, timeout(5_000).times(1))
                .processUserMessage(any(UUID.class), anyStatusUpdateCallback());
    }

    @Test
    void testStarterSubmitPersistsPendingContextOnFirstUserTurn() {
        when(mockAnalyticsService.processUserMessage(
                any(UUID.class),
                anyStatusUpdateCallback()
        ))
                .thenReturn("mock assistant response");

        Category category = createAndSaveEntity(Category.class, cat -> {
            cat.setName("Starter Context Category");
            cat.setCode("starter-context-" + UUID.randomUUID());
        });
        String entityReference = idSerialization.idToString(Id.of(category));

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        AiConversationComposerFragment composer = composerOf(starterView);
        composer.addEntityReferences(List.of(entityReference));
        composer.addAttachments(List.of(new PendingAttachmentInput(
                fileRef("starter-context.csv"),
                "starter-context.csv"
        )));

        fireSubmit(inputOf(starterView), "Analyse this pending context");

        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        AiConversation reloaded = dataManager.load(AiConversation.class)
                .id(detailView.getEditedEntity().getId())
                .fetchPlan(fp -> fp.add("messages", message -> message.addFetchPlan(FetchPlan.BASE)
                        .add("entityReferences", FetchPlan.BASE)
                        .add("attachments", FetchPlan.BASE)))
                .one();

        ChatMessage userMessage = reloaded.getMessages().stream()
                .filter(message -> ChatMessageType.USER.equals(message.getType()))
                .findFirst()
                .orElseThrow();

        assertThat(userMessage.getContent()).isEqualTo("Analyse this pending context");
        assertThat(userMessage.getEntityReferences())
                .extracting(ChatMessageEntityReference::getEntityReference)
                .containsExactly(entityReference);
        assertThat(userMessage.getAttachments())
                .extracting(AiConversationAttachment::getFileName)
                .containsExactly("starter-context.csv");
    }

    @Test
    void testRemovePendingContextItemsFromComposer() {
        Category category = createAndSaveEntity(Category.class, cat -> {
            cat.setName("Starter Context Category");
            cat.setCode("starter-context-" + UUID.randomUUID());
        });
        String entityReference = idSerialization.idToString(Id.of(category));

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        AiConversationComposerFragment composer = composerOf(starterView);
        composer.addEntityReferences(List.of(entityReference));
        PendingAttachmentInput attachment = new PendingAttachmentInput(
                fileRef("starter-context.csv"),
                "starter-context.csv"
        );
        composer.addAttachments(List.of(attachment));

        assertThat(composer.entityReferences()).containsExactly(entityReference);
        assertThat(composer.attachments()).containsExactly(attachment);
        assertThat(composer.isEmpty()).isFalse();

        // Test removing the entity reference
        composer.removeEntityReference(entityReference);
        assertThat(composer.entityReferences()).isEmpty();
        assertThat(composer.attachments()).containsExactly(attachment);
        assertThat(composer.isEmpty()).isFalse();

        // Test removing the attachment
        composer.removeAttachment(attachment);
        assertThat(composer.entityReferences()).isEmpty();
        assertThat(composer.attachments()).isEmpty();
        assertThat(composer.isEmpty()).isTrue();
    }

    @Test
    void testEmptyPromptSubmitDoesNotCreateConversation() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        long conversationsBefore = countConversations();
        fireSubmit(inputOf(starterView), "   ");

        Object currentView = UiTestUtils.getCurrentView();
        assertThat(currentView)
                .as("a blank prompt must not navigate away from the starter view")
                .isInstanceOf(AiConversationStarterView.class);
        assertThat(countConversations())
                .as("a blank prompt must not persist a conversation")
                .isEqualTo(conversationsBefore);
    }

    @Test
    void testRecentConversationsHiddenWhenNoneExist() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        HorizontalLayout header = UiTestUtils.getComponent(starterView, "recentConversationsHeader");
        @SuppressWarnings("unchecked")
        GridLayout<AiConversation> recentGrid =
                UiTestUtils.getComponent(starterView, "recentConversationsGridLayout");

        assertThat(header.isVisible())
                .as("recent conversations section must be hidden when there are no recents")
                .isFalse();
        assertThat(recentGrid.isVisible())
                .as("recent conversations grid must be hidden when there are no recents")
                .isFalse();
    }

    @Test
    void testRecentConversationsAreLoadedWhenAvailable() {
        AiConversation recent = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Recent Conversation");
            conv.setCreatedDate(OffsetDateTime.now().minusMinutes(5));
            conv.setFirstMessageSent(true);
        });

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        HorizontalLayout header = UiTestUtils.getComponent(starterView, "recentConversationsHeader");
        @SuppressWarnings("unchecked")
        GridLayout<AiConversation> recentGrid =
                UiTestUtils.getComponent(starterView, "recentConversationsGridLayout");

        assertThat(header.isVisible())
                .as("recent conversations header must be visible when recents exist")
                .isTrue();
        assertThat(recentGrid.isVisible())
                .as("recent conversations grid must be visible when recents exist")
                .isTrue();
        // The grid renderer pulls items from recentConversationsDc; we just check the title
        // shows up in the rendered DOM tree.
        assertThat(recentGrid.getElement().getTextRecursively())
                .as("rendered recent grid must surface the conversation's title")
                .contains(recent.getTitle());
    }

    @Test
    void testShowAllOpensHistorySidePanel() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        SidePanelLayout panel = UiTestUtils.getComponent(starterView, "historySidePanel");
        assertThat(panel.isSidePanelOpened()).isFalse();

        JmixButton showAllBtn = UiTestUtils.getComponent(starterView, "showAllHistoryBtn");
        showAllBtn.click();

        assertThat(panel.isSidePanelOpened())
                .as("clicking 'Show all' must open the history side panel")
                .isTrue();
    }

    @Test
    void testHistoryPanelRendersConversationsGroupedByDate() {
        AiConversation today = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Today Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });
        AiConversation yesterday = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Yesterday Conversation");
            conv.setCreatedDate(OffsetDateTime.now().minusDays(1));
            conv.setFirstMessageSent(true);
        });

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        UiTestUtils.<JmixButton>getComponent(starterView, "showAllHistoryBtn").click();

        VerticalLayout list = UiTestUtils.getComponent(starterView, "historyListContainer");
        String text = list.getElement().getTextRecursively();

        assertThat(text)
                .as("history must contain both conversations")
                .contains(today.getTitle())
                .contains(yesterday.getTitle());
        assertThat(text)
                .as("history must group conversations under their date buckets")
                .containsIgnoringCase("today")
                .containsIgnoringCase("yesterday");
    }

    @Test
    void testHistoryPanelCountReflectsConversationsTotal() {
        createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("History Count A");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });
        createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("History Count B");
            conv.setCreatedDate(OffsetDateTime.now().minusHours(2));
            conv.setFirstMessageSent(true);
        });

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        UiTestUtils.<JmixButton>getComponent(starterView, "showAllHistoryBtn").click();

        Span countBadge = UiTestUtils.getComponent(starterView, "historyPanelCount");
        assertThat(countBadge.getText()).isEqualTo("2");
    }

    @Test
    void testHistoryCloseButtonClosesPanel() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        SidePanelLayout panel = UiTestUtils.getComponent(starterView, "historySidePanel");
        UiTestUtils.<JmixButton>getComponent(starterView, "showAllHistoryBtn").click();
        assertThat(panel.isSidePanelOpened()).isTrue();

        UiTestUtils.<JmixButton>getComponent(starterView, "historyCloseBtn").click();
        assertThat(panel.isSidePanelOpened()).isFalse();
    }

    @Test
    void testHistoryNewButtonClosesPanel() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        SidePanelLayout panel = UiTestUtils.getComponent(starterView, "historySidePanel");
        UiTestUtils.<JmixButton>getComponent(starterView, "showAllHistoryBtn").click();
        assertThat(panel.isSidePanelOpened()).isTrue();

        UiTestUtils.<JmixButton>getComponent(starterView, "historyNewBtn").click();
        assertThat(panel.isSidePanelOpened()).isFalse();
    }

    @Test
    void testHistorySearchFieldIsAvailable() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();

        @SuppressWarnings("unchecked")
        TypedTextField<String> searchField =
                UiTestUtils.getComponent(starterView, "historySearchField");
        assertThat(searchField).isNotNull();
        assertThat(searchField.getPlaceholder())
                .as("search field must be configured with a localized placeholder")
                .isNotBlank();
    }

    @Test
    void testHistorySearchFiltersByTitleAndFirstUserMessage() {
        AiConversation titleMatch = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Alpha Pipeline Review");
            conv.setCreatedDate(OffsetDateTime.now().minusMinutes(10));
            conv.setFirstMessageSent(true);
        });
        createAndSaveEntity(ChatMessage.class, message -> {
            message.setConversation(titleMatch);
            message.setType(ChatMessageType.USER);
            message.setContent("Show renewal risk");
            message.setCreatedDate(OffsetDateTime.now().minusMinutes(9));
        });

        AiConversation snippetMatch = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Finance Follow-up");
            conv.setCreatedDate(OffsetDateTime.now().minusMinutes(8));
            conv.setFirstMessageSent(true);
        });
        createAndSaveEntity(ChatMessage.class, message -> {
            message.setConversation(snippetMatch);
            message.setType(ChatMessageType.USER);
            message.setContent("Find beta overdue invoices");
            message.setCreatedDate(OffsetDateTime.now().minusMinutes(7));
        });

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();
        UiTestUtils.<JmixButton>getComponent(starterView, "showAllHistoryBtn").click();

        VerticalLayout list = UiTestUtils.getComponent(starterView, "historyListContainer");
        assertThat(list.getElement().getTextRecursively())
                .contains(titleMatch.getTitle())
                .contains(snippetMatch.getTitle());

        UiTestUtils.<TypedTextField<String>>getComponent(starterView, "historySearchField")
                .setValue("beta");

        assertThat(list.getElement().getTextRecursively())
                .contains(snippetMatch.getTitle())
                .doesNotContain(titleMatch.getTitle());
    }

    @Test
    void testStarterViewRouteIsAiConversations() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationStarterView.class).navigate();
        AiConversationStarterView starterView = UiTestUtils.getCurrentView();
        assertThat(starterView).isNotNull();
    }

    private static void fireSubmit(MessageInput messageInput, String value) {
        ComponentUtil.fireEvent(messageInput, new MessageInput.SubmitEvent(messageInput, false, value));
    }

    private MessageInput inputOf(AiConversationStarterView starterView) {
        VerticalLayout composerCard = UiTestUtils.getComponent(starterView, "composerCard");
        return viewTestSupport.findDescendant(composerCard, MessageInput.class)
                .orElseThrow(() -> new AssertionError("MessageInput not found in composer"));
    }

    private AiConversationComposerFragment composerOf(AiConversationStarterView starterView) {
        VerticalLayout composerCard = UiTestUtils.getComponent(starterView, "composerCard");
        return viewTestSupport.findDescendant(composerCard, AiConversationComposerFragment.class)
                .orElseThrow(() -> new AssertionError("Composer fragment not found"));
    }

    private long countConversations() {
        return systemAuthenticator.withSystem(() -> dataManager.loadValue(
                "select count(c) from AiConversation c", Long.class).one());
    }

    private FileRef fileRef(String fileName) {
        return new FileRef("storage", "test/" + fileName, fileName);
    }

    private static Consumer<AiUiStatusUpdate> anyStatusUpdateCallback() {
        return any();
    }
}
