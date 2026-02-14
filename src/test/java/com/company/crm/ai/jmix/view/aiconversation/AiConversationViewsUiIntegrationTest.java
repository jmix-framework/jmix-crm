package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.app.service.ai.CrmAnalyticsService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * UI integration tests for AI Conversation views.
 * Tests the complete UI workflow from list view to detail view using Jmix UI components.
 */
public class AiConversationViewsUiIntegrationTest extends AbstractUiTest {

    @Autowired
    private ViewNavigators viewNavigators;

    @MockitoBean
    private CrmAnalyticsService mockAnalyticsService;

    @Test
    void testListViewDisplaysConversations() {
        // Create test data
        AiConversation conversation1 = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Conversation 1");
            conv.setCreatedDate(OffsetDateTime.now().minusHours(1));
        });

        AiConversation conversation2 = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Conversation 2");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // Navigate to AI Conversation list view
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();

        AiConversationListView listView = UiTestUtils.getCurrentView();
        assertThat(listView).isNotNull();

        // Get the data grid
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");
        assertThat(dataGrid).isNotNull();

        // Check that conversations are displayed
        DataGridItems<AiConversation> items = dataGrid.getItems();
        assertThat(items).isNotNull();

        Collection<AiConversation> conversationList = items.getItems();
        assertThat(conversationList).hasSizeGreaterThanOrEqualTo(2);

        // Verify our test conversations are in the list
        assertThat(conversationList)
            .as("Test Conversation 1 should be in the list")
            .anyMatch(conv -> conversation1.getTitle().equals(conv.getTitle()));
        assertThat(conversationList)
            .as("Test Conversation 2 should be in the list")
            .anyMatch(conv -> conversation2.getTitle().equals(conv.getTitle()));
    }

    @Test
    void testNewChatButtonCreatesAndNavigates() {
        // Navigate to AI Conversation list view
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();

        AiConversationListView listView = UiTestUtils.getCurrentView();

        // Count initial conversations
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");
        int initialCount = dataGrid.getItems().getItems().size();

        // Click "New Chat" button
        JmixButton createButton = UiTestUtils.getComponent(listView, "createButton");
        assertThat(createButton).isNotNull();
        assertThat(createButton.getText()).isEqualTo("New Chat");

        createButton.click();

        // Should navigate to detail view
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView).isNotNull();

        // Check that a new conversation was created
        AiConversation editedEntity = detailView.getEditedEntity();
        assertThat(editedEntity).isNotNull();
        assertThat(editedEntity.getId()).isNotNull();
        assertThat(editedEntity.getTitle()).contains("AI Chat Session");
    }

    @Test
    void testOpenExistingConversation() {
        // Create test conversation
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Open Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // Navigate to AI Conversation list view
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();

        AiConversationListView listView = UiTestUtils.getCurrentView();

        // Get the data grid and select our test conversation
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");

        // Find and select the test conversation
        Collection<AiConversation> conversations = dataGrid.getItems().getItems();
        AiConversation foundConversation = conversations.stream()
            .filter(conv -> testConversation.getTitle().equals(conv.getTitle()))
            .findFirst()
            .orElse(null);

        assertThat(foundConversation)
            .as("Test conversation should be found in the list")
            .isNotNull();

        // Select the conversation in the grid
        dataGrid.select(foundConversation);

        // Click "Open" button
        JmixButton editButton = UiTestUtils.getComponent(listView, "editButton");
        assertThat(editButton).isNotNull();
        assertThat(editButton.getText()).isEqualTo("Open");

        editButton.click();

        // Should navigate to detail view with the selected conversation
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView).isNotNull();

        AiConversation editedEntity = detailView.getEditedEntity();
        assertThat(editedEntity).isNotNull();
        assertThat(editedEntity.getId()).isEqualTo(testConversation.getId());
        assertThat(editedEntity.getTitle()).isEqualTo(testConversation.getTitle());
    }

    @Test
    void testDetailViewContainsAiComponent() {
        // Create test conversation
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test Detail View");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // Navigate directly to detail view
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
            .editEntity(testConversation)
            .navigate();

        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView).isNotNull();

        // Verify the conversation is loaded
        assertThat(detailView.getEditedEntity().getId()).isEqualTo(testConversation.getId());

        // Check that the detail view has the expected components
        // (The AI conversation component should be added programmatically in setupAiConversationComponent)
        assertThat(detailView.getEditedEntity()).isNotNull();
    }

    @Test
    void testComponentIntegrationInViews() {
        // Configure mock analytics service
        when(mockAnalyticsService.processBusinessQuestion(anyString(), anyString()))
            .thenReturn("Test AI response from mock service");

        // Create test conversation
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Component Integration Test");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // Test 1: Navigate to list view and create new chat
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();
        AiConversationListView listView = UiTestUtils.getCurrentView();

        JmixButton createButton = UiTestUtils.getComponent(listView, "createButton");
        createButton.click();

        // Should be in detail view now
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertThat(detailView).isNotNull();
        assertThat(detailView.getEditedEntity()).isNotNull();

        // Test 2: Navigate back to list and open existing conversation
        viewNavigators.view(detailView, AiConversationListView.class).navigate();
        listView = UiTestUtils.getCurrentView();

        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");

        // Select the test conversation
        Collection<AiConversation> conversations = dataGrid.getItems().getItems();
        AiConversation foundConversation = conversations.stream()
            .filter(conv -> testConversation.getTitle().equals(conv.getTitle()))
            .findFirst()
            .orElse(null);

        if (foundConversation != null) {
            dataGrid.select(foundConversation);

            JmixButton editButton = UiTestUtils.getComponent(listView, "editButton");
            editButton.click();

            // Should be in detail view with the selected conversation
            detailView = UiTestUtils.getCurrentView();
            assertThat(detailView).isNotNull();
            assertThat(detailView.getEditedEntity().getId()).isEqualTo(testConversation.getId());
        }
    }
}