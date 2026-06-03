package com.company.crm.test.ai.view.conversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.CrmAnalyticsService;
import com.company.crm.ai.view.conversation.AiConversationDetailView;
import com.company.crm.ai.view.conversation.AiConversationListView;
import com.company.crm.model.catalog.category.Category;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for AI Conversation views.
 * Tests the complete UI workflow from list view to detail view using Jmix UI components.
 */
public class AiConversationViewsUiTest extends AbstractUiTest {

    @Autowired
    private ViewNavigators viewNavigators;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private IdSerialization idSerialization;

    @MockitoBean
    private CrmAnalyticsService mockAnalyticsService;

    @Test
    void testListViewDisplaysConversations() {
        // given
        AiConversation conversation1 = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Conversation 1");
            conv.setCreatedDate(OffsetDateTime.now().minusHours(1));
            conv.setFirstMessageSent(true);
        });

        AiConversation conversation2 = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Conversation 2");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });

        // when
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();

        AiConversationListView listView = UiTestUtils.getCurrentView();
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");
        DataGridItems<AiConversation> items = dataGrid.getItems();
        Collection<AiConversation> conversationList = items.getItems();

        // then
        assertThat(conversationList).hasSizeGreaterThanOrEqualTo(2);
        assertThat(conversationList.stream().map(AiConversation::getTitle).toList())
                .contains(conversation1.getTitle(), conversation2.getTitle());
    }

    @Test
    void testOpenExistingConversation() {
        // given
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Open Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });

        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();
        // when
        AiConversationListView listView = UiTestUtils.getCurrentView();
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");
        Collection<AiConversation> conversations = dataGrid.getItems().getItems();
        AiConversation foundConversation = conversations.stream()
                .filter(conv -> testConversation.getTitle().equals(conv.getTitle()))
                .findFirst()
                .orElseThrow();
        dataGrid.select(foundConversation);
        JmixButton readButton = UiTestUtils.getComponent(listView, "readButton");
        readButton.click();

        // then
        assertThat(readButton.getText()).isEqualTo("View");
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView.isReadOnly()).isTrue();
        AiConversation editedEntity = detailView.getEditedEntity();
        assertThat(editedEntity.getId()).isEqualTo(testConversation.getId());
        assertThat(editedEntity.getTitle()).isEqualTo(testConversation.getTitle());
    }

    @Test
    void testOpenTimelineViewDisplaysInlineAttachmentEvent() {
        // given
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Timeline Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });

        ChatMessage message = createAndSaveEntity(ChatMessage.class, msg -> {
            msg.setConversation(testConversation);
            msg.setType(ChatMessageType.ASSISTANT);
            msg.setContent("CRM AI added Timeline CSV to context");
            msg.setCreatedDate(OffsetDateTime.now());
        });

        createAndSaveEntity(AiConversationAttachment.class, att -> {
            att.setMessage(message);
            att.setFile(new FileRef("storage", "2026/05/15/timeline.csv", "timeline.csv"));
            att.setFileName("timeline.csv");
            att.setTitle("Timeline CSV");
            att.setOrigin(AiAttachmentOrigin.AI_GENERATED);
            att.setCreatedDate(OffsetDateTime.now());
        });

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(testConversation)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView.getEditedEntity().getId()).isEqualTo(testConversation.getId());
        openContextPanel(detailView);
        assertThat(contextPanelText(detailView)).contains("Timeline CSV");
    }

    @Test
    void testOpenTimelineViewDisplaysEntityReferenceEvent() {
        // given
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Timeline Entity Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });
        Category category = createAndSaveEntity(Category.class, cat -> {
            cat.setName("Timeline Category");
            cat.setCode("timeline-category-" + UUID.randomUUID());
        });
        String entityReference = idSerialization.idToString(Id.of(category));

        ChatMessage message = createAndSaveEntity(ChatMessage.class, msg -> {
            msg.setConversation(testConversation);
            msg.setType(ChatMessageType.ASSISTANT);
            msg.setContent("added CRM entity Categories \"Timeline Category\" to context");
            msg.setCreatedDate(OffsetDateTime.now());
        });

        createAndSaveEntity(ChatMessageEntityReference.class, ref -> {
            ref.setMessage(message);
            ref.setEntityReference(entityReference);
        });

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(testConversation)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView.getEditedEntity().getId()).isEqualTo(testConversation.getId());
        openContextPanel(detailView);
        assertThat(contextPanelText(detailView)).contains("Timeline Category");
    }

    @Test
    void testDetailViewContainsAiComponent() {
        // given
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Detail View");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(testConversation)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView.getEditedEntity().getId()).isEqualTo(testConversation.getId());
        assertThat(detailView.getEditedEntity().getTitle()).isEqualTo("Test Detail View");
    }

    @Test
    void testComponentIntegrationInViews() {
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Component Integration Test");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });

        // when
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();
        AiConversationListView listView = UiTestUtils.getCurrentView();

        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");

        // Select the test conversation
        Collection<AiConversation> conversations = dataGrid.getItems().getItems();
        AiConversation foundConversation = conversations.stream()
                .filter(conv -> testConversation.getTitle().equals(conv.getTitle()))
                .findFirst()
                .orElseThrow();
        dataGrid.select(foundConversation);

        JmixButton readButton = UiTestUtils.getComponent(listView, "readButton");
        readButton.click();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(readButton.getText()).isEqualTo("View");
        assertThat(detailView.isReadOnly()).isTrue();
        assertThat(detailView.getEditedEntity().getId()).isEqualTo(testConversation.getId());
    }

    @Test
    void testDetailViewDisplaysAttachmentDownloadLink() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Attachment View Test");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        ChatMessage message = createAndSaveEntity(ChatMessage.class, msg -> {
            msg.setConversation(conversation);
            msg.setType(ChatMessageType.ASSISTANT);
            msg.setContent("Attachment message");
            msg.setCreatedDate(OffsetDateTime.now());
        });

        createAndSaveEntity(AiConversationAttachment.class, att -> {
            att.setMessage(message);
            att.setFile(new FileRef("storage", "2026/02/22/report.html", "report.html"));
            att.setFileName("report.html");
            att.setOrigin(AiAttachmentOrigin.AI_GENERATED);
            att.setCreatedDate(OffsetDateTime.now());
        });

        AiConversation conversationWithAttachments = dataManager.load(AiConversation.class)
                .id(conversation.getId())
                .fetchPlan(fp -> fp.add("messages", sub -> sub.add("attachments", attSub -> attSub.addFetchPlan("_base"))))
                .one();

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversationWithAttachments)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        openContextPanel(detailView);
        assertThat(contextPanelText(detailView)).contains("report.html");
    }

    @Test
    void testContextToggleButtonHasLocalizedLabelAndNoBadgeWhenEmpty() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Empty Context Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversation)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        JmixButton toggleButton = UiTestUtils.getComponent(detailView, "attachmentsToggleBtn");
        assertThat(toggleButton.getText()).isEqualTo("Context");
        assertThat(toggleButton.getSuffixComponent()).isNull();
    }

    @Test
    void testContextToggleButtonShowsBadgeWithAggregatedContextCount() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Context Badge Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });

        Category category = createAndSaveEntity(Category.class, cat -> {
            cat.setName("Badge Category");
            cat.setCode("badge-category-" + UUID.randomUUID());
        });
        String entityReference = idSerialization.idToString(Id.of(category));

        ChatMessage message = createAndSaveEntity(ChatMessage.class, msg -> {
            msg.setConversation(conversation);
            msg.setType(ChatMessageType.ASSISTANT);
            msg.setContent("Mixed-context message");
            msg.setCreatedDate(OffsetDateTime.now());
        });

        createAndSaveEntity(ChatMessageEntityReference.class, ref -> {
            ref.setMessage(message);
            ref.setEntityReference(entityReference);
        });

        createAndSaveEntity(AiConversationAttachment.class, att -> {
            att.setMessage(message);
            att.setFile(new FileRef("storage", "2026/05/16/generated.csv", "generated.csv"));
            att.setFileName("generated.csv");
            att.setOrigin(AiAttachmentOrigin.AI_GENERATED);
            att.setCreatedDate(OffsetDateTime.now());
        });

        createAndSaveEntity(AiConversationAttachment.class, att -> {
            att.setMessage(message);
            att.setFile(new FileRef("storage", "2026/05/16/uploaded.pdf", "uploaded.pdf"));
            att.setFileName("uploaded.pdf");
            att.setOrigin(AiAttachmentOrigin.USER_UPLOADED);
            att.setCreatedDate(OffsetDateTime.now());
        });

        AiConversation conversationWithContext = dataManager.load(AiConversation.class)
                .id(conversation.getId())
                .fetchPlan(fp -> fp.add("messages", sub -> sub
                        .add("attachments", attSub -> attSub.addFetchPlan("_base"))
                        .add("entityReferences", refSub -> refSub.addFetchPlan("_base"))))
                .one();

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversationWithContext)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        JmixButton toggleButton = UiTestUtils.getComponent(detailView, "attachmentsToggleBtn");
        assertThat(toggleButton.getText()).isEqualTo("Context");
        Component suffix = toggleButton.getSuffixComponent();
        assertThat(suffix).isInstanceOf(Span.class);
        assertThat(((Span) suffix).getText()).isEqualTo("3");
    }

    @Test
    void testDetailViewDisplaysAttachments() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Attachment View Test 2");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        ChatMessage message = createAndSaveEntity(ChatMessage.class, msg -> {
            msg.setConversation(conversation);
            msg.setType(ChatMessageType.ASSISTANT);
            msg.setContent("Attachment message 2");
            msg.setCreatedDate(OffsetDateTime.now());
        });

        createAndSaveEntity(AiConversationAttachment.class, att -> {
            att.setMessage(message);
            att.setFile(new FileRef("storage", "2026/02/22/report.csv", "report.csv"));
            att.setFileName("report.csv");
            att.setTitle("Risk Allocation CSV");
            att.setOrigin(AiAttachmentOrigin.AI_GENERATED);
            att.setCreatedDate(OffsetDateTime.now());
        });

        AiConversation conversationWithAttachments = dataManager.load(AiConversation.class)
                .id(conversation.getId())
                .fetchPlan(fp -> fp.add("messages", sub -> sub.add("attachments", attSub -> attSub.addFetchPlan("_base"))))
                .one();

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversationWithAttachments)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        openContextPanel(detailView);
        assertThat(contextPanelText(detailView)).contains("Risk Allocation CSV");
        assertThat(detailView.getEditedEntity().getTitle()).isEqualTo("Attachment View Test 2");
    }

    private void openContextPanel(AiConversationDetailView detailView) {
        JmixButton toggleButton = UiTestUtils.getComponent(detailView, "attachmentsToggleBtn");
        toggleButton.click();
    }

    private String contextPanelText(AiConversationDetailView detailView) {
        VerticalLayout contextPanelContent = UiTestUtils.getComponent(detailView, "contextSidePanelContent");
        return contextPanelContent.getElement().getTextRecursively();
    }

}
