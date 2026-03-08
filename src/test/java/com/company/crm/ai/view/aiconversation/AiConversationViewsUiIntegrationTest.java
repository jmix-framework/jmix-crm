package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.AbstractUiTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.service.CrmAnalyticsService;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.upload.receiver.FileTemporaryStorageBuffer;
import io.jmix.flowui.component.upload.receiver.TemporaryStorageFileData;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.upload.TemporaryStorage;
import io.jmix.flowui.upload.TemporaryStorageImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UI integration tests for AI Conversation views.
 * Tests the complete UI workflow from list view to detail view using Jmix UI components.
 */
public class AiConversationViewsUiIntegrationTest extends AbstractUiTest {

    @Autowired
    private ViewNavigators viewNavigators;

    @Autowired
    private DataManager dataManager;

    @MockitoBean
    private CrmAnalyticsService mockAnalyticsService;
    @MockitoBean
    private TemporaryStorageImpl temporaryStorage;
    @MockitoBean
    private FileStorageLocator fileStorageLocator;

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
    void testNewChatButtonCreatesAndNavigates() {
        // given
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();

        // when
        AiConversationListView listView = UiTestUtils.getCurrentView();
        JmixButton createButton = UiTestUtils.getComponent(listView, "createButton");
        createButton.click();

        // then
        assertThat(createButton.getText()).isEqualTo("New Chat");
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        AiConversation editedEntity = detailView.getEditedEntity();
        assertThat(editedEntity.getId()).isNotNull();
        assertThat(dataManager.load(AiConversation.class).id(editedEntity.getId()).optional()).isPresent();
        assertThat(editedEntity.getTitle()).contains("New Chat");
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
        JmixButton editButton = UiTestUtils.getComponent(listView, "editButton");
        editButton.click();

        // then
        assertThat(editButton.getText()).isEqualTo("Open");
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        AiConversation editedEntity = detailView.getEditedEntity();
        assertThat(editedEntity.getId()).isEqualTo(testConversation.getId());
        assertThat(editedEntity.getTitle()).isEqualTo(testConversation.getTitle());
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
        assertThat(detailView.getRenderedAttachmentCount()).isEqualTo(0);
    }

    @Test
    void testComponentIntegrationInViews() {
        // given
        when(mockAnalyticsService.processBusinessQuestion(anyString(), anyString()))
            .thenReturn("Test AI response from mock service");

        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Component Integration Test");
            conv.setCreatedDate(OffsetDateTime.now());
            conv.setFirstMessageSent(true);
        });

        // when
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();
        AiConversationListView listView = UiTestUtils.getCurrentView();

        JmixButton createButton = UiTestUtils.getComponent(listView, "createButton");
        createButton.click();

        // Should be in detail view now
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(createButton.getText()).isEqualTo("New Chat");
        UUID draftConversationId = detailView.getEditedEntity().getId();
        assertThat(draftConversationId).isNotNull();
        assertThat(dataManager.load(AiConversation.class).id(draftConversationId).optional()).isPresent();

        // Test 2: Navigate back to list and open existing conversation
        viewNavigators.view(detailView, AiConversationListView.class).navigate();
        listView = UiTestUtils.getCurrentView();

        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");

        // Select the test conversation
        Collection<AiConversation> conversations = dataGrid.getItems().getItems();
        AiConversation foundConversation = conversations.stream()
            .filter(conv -> testConversation.getTitle().equals(conv.getTitle()))
            .findFirst()
            .orElseThrow();
        dataGrid.select(foundConversation);

        JmixButton editButton = UiTestUtils.getComponent(listView, "editButton");
        editButton.click();

        // then
        detailView = UiTestUtils.getCurrentView();
        assertThat(editButton.getText()).isEqualTo("Open");
        assertThat(detailView.getEditedEntity().getId()).isEqualTo(testConversation.getId());
    }

    @Test
    void testDetailViewDisplaysAttachmentDownloadLink() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Attachment View Test");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        AiConversationAttachment attachment = createAndSaveEntity(AiConversationAttachment.class, att -> {
            att.setConversation(conversation);
            att.setFile(new FileRef("storage", "2026/02/22/report.html", "report.html"));
            att.setFileName("report.html");
            att.setType(AiAttachmentType.AI_GENERATED);
            att.setCreatedDate(OffsetDateTime.now());
        });

        AiConversation conversationWithAttachments = dataManager.load(AiConversation.class)
                .id(conversation.getId())
                .fetchPlan(fp -> fp.add("attachments", sub -> sub.addFetchPlan("_base")))
                .one();

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversationWithAttachments)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView.getRenderedAttachmentCount()).isGreaterThan(0);
        assertThat(detailView.hasRenderedAttachment(attachment.getId())).isTrue();
    }

    @Test
    void testDetailViewDisplaysAttachments() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Attachment View Test 2");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        AiConversationAttachment attachment = createAndSaveEntity(AiConversationAttachment.class, att -> {
            att.setConversation(conversation);
            att.setFile(new FileRef("storage", "2026/02/22/report.csv", "report.csv"));
            att.setFileName("report.csv");
            att.setTitle("Risk Allocation CSV");
            att.setType(AiAttachmentType.AI_GENERATED);
            att.setCreatedDate(OffsetDateTime.now());
        });

        AiConversation conversationWithAttachments = dataManager.load(AiConversation.class)
                .id(conversation.getId())
                .fetchPlan(fp -> fp.add("attachments", sub -> sub.addFetchPlan("_base")))
                .one();

        // when
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversationWithAttachments)
                .navigate();

        // then
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView.getRenderedAttachmentCount()).isGreaterThan(0);
        assertThat(detailView.hasRenderedAttachment(attachment.getId())).isTrue();
        assertThat(detailView.getEditedEntity().getTitle()).isEqualTo("Attachment View Test 2");
    }

    @Test
    void testAttachmentUploadPersistsUserUploadedAttachment() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Upload Integration Test");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
                .editEntity(conversation)
                .navigate();
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();

        UUID tempFileId = UUID.randomUUID();
        TemporaryStorage.FileInfo fileInfo = new TemporaryStorage.FileInfo(new File("test.tmp"), tempFileId);
        TemporaryStorageFileData fileData = new TemporaryStorageFileData("analysis.csv", "text/csv", fileInfo);

        FileTemporaryStorageBuffer receiver = mock(FileTemporaryStorageBuffer.class);
        when(receiver.getFileData()).thenReturn(fileData);
        Upload upload = mock(Upload.class);
        when(upload.getReceiver()).thenReturn(receiver);
        SucceededEvent event = new SucceededEvent(upload, "analysis.csv", "text/csv", 12);

        FileStorage fileStorage = mock(FileStorage.class);
        FileRef uploadedFileRef = new FileRef("crm", "2026/02/23/analysis.csv", "analysis.csv");
        when(fileStorageLocator.getByName("crm")).thenReturn(fileStorage);
        when(temporaryStorage.putFileIntoStorage(tempFileId, "analysis.csv", fileStorage))
                .thenReturn(uploadedFileRef);
        when(mockAnalyticsService.processAttachmentUpload(anyString(), any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn("AI upload processed");

        // when
        detailView.onAttachmentUploadSucceeded(event);

        // then
        AiConversationAttachment savedAttachment = dataManager.load(AiConversationAttachment.class)
                .query("select e from AiConversationAttachment e where e.conversation.id = :conversationId and e.fileName = :fileName")
                .parameter("conversationId", conversation.getId())
                .parameter("fileName", "analysis.csv")
                .one();

        assertThat(savedAttachment.getType()).isEqualTo(AiAttachmentType.USER_UPLOADED);
        assertThat(savedAttachment.getFileName()).isEqualTo("analysis.csv");
        assertThat(savedAttachment.getFile()).isEqualTo(uploadedFileRef);
        verify(temporaryStorage).putFileIntoStorage(tempFileId, "analysis.csv", fileStorage);
    }
}
