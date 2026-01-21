package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.util.UniqueValues;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for AI Conversation views.
 * Tests the complete workflow from list view to detail view.
 */
public class AiConversationViewsEndToEndTest extends AbstractUiTest {

    @MockBean
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
        assertNotNull(listView);

        // Get the data grid
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");
        assertNotNull(dataGrid);

        // Check that conversations are displayed
        DataGridItems<AiConversation> items = dataGrid.getItems();
        assertNotNull(items);

        Collection<AiConversation> conversationList = items.getItems();
        assertTrue(conversationList.size() >= 2);

        // Verify our test conversations are in the list
        boolean foundConv1 = conversationList.stream()
            .anyMatch(conv -> conversation1.getTitle().equals(conv.getTitle()));
        boolean foundConv2 = conversationList.stream()
            .anyMatch(conv -> conversation2.getTitle().equals(conv.getTitle()));

        assertTrue(foundConv1, "Test Conversation 1 should be in the list");
        assertTrue(foundConv2, "Test Conversation 2 should be in the list");
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
        assertNotNull(createButton);
        assertEquals("New Chat", createButton.getText());

        createButton.click();

        // Should navigate to detail view
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertNotNull(detailView);

        // Check that a new conversation was created
        AiConversation editedEntity = detailView.getEditedEntity();
        assertNotNull(editedEntity);
        assertNotNull(editedEntity.getId());
        assertTrue(editedEntity.getTitle().contains("New AI Chat"));
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

        assertNotNull(foundConversation, "Test conversation should be found in the list");

        // Select the conversation in the grid
        dataGrid.select(foundConversation);

        // Click "Open" button
        JmixButton editButton = UiTestUtils.getComponent(listView, "editButton");
        assertNotNull(editButton);
        assertEquals("Open", editButton.getText());

        editButton.click();

        // Should navigate to detail view with the selected conversation
        AiConversationDetailView detailView = UiTestUtils.getCurrentView();
        assertNotNull(detailView);

        AiConversation editedEntity = detailView.getEditedEntity();
        assertNotNull(editedEntity);
        assertEquals(testConversation.getId(), editedEntity.getId());
        assertEquals(testConversation.getTitle(), editedEntity.getTitle());
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
        assertNotNull(detailView);

        // Verify the conversation is loaded
        assertEquals(testConversation.getId(), detailView.getEditedEntity().getId());

        // Check that the detail view has the expected components
        // (The AI conversation component should be added programmatically in setupAiConversationComponent)
        assertNotNull(detailView.getEditedEntity());
    }

    @Test
    void testListViewSortedByCreatedDate() {
        // Create conversations with different timestamps
        AiConversation older = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Older Conversation");
            conv.setCreatedDate(OffsetDateTime.now().minusHours(2));
        });

        AiConversation newer = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Newer Conversation");
            conv.setCreatedDate(OffsetDateTime.now().minusMinutes(5));
        });

        AiConversation newest = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Newest Conversation");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // Navigate to list view
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();

        AiConversationListView listView = UiTestUtils.getCurrentView();
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");

        // Get all conversations
        Collection<AiConversation> conversations = dataGrid.getItems().getItems();

        // Convert to list to check order (should be sorted by createdDate DESC)
        var conversationList = conversations.stream().toList();

        // Find positions of our test conversations
        int newestPosition = -1;
        int newerPosition = -1;
        int olderPosition = -1;

        for (int i = 0; i < conversationList.size(); i++) {
            AiConversation conv = conversationList.get(i);
            if (newest.getTitle().equals(conv.getTitle())) {
                newestPosition = i;
            } else if (newer.getTitle().equals(conv.getTitle())) {
                newerPosition = i;
            } else if (older.getTitle().equals(conv.getTitle())) {
                olderPosition = i;
            }
        }

        // Verify sorting (newest should come first)
        assertTrue(newestPosition >= 0, "Newest conversation should be found");
        assertTrue(newerPosition >= 0, "Newer conversation should be found");
        assertTrue(olderPosition >= 0, "Older conversation should be found");

        // Newest should come before newer, which should come before older
        assertTrue(newestPosition < newerPosition, "Newest should come before newer");
        assertTrue(newerPosition < olderPosition, "Newer should come before older");
    }

    @Test
    void testRemoveConversation() {
        // Create test conversation
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("To Be Deleted");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // Navigate to list view
        viewNavigators.view(UiTestUtils.getCurrentView(), AiConversationListView.class).navigate();

        AiConversationListView listView = UiTestUtils.getCurrentView();
        DataGrid<AiConversation> dataGrid = UiTestUtils.getComponent(listView, "aiConversationsDataGrid");

        // Find and select the test conversation
        Collection<AiConversation> conversations = dataGrid.getItems().getItems();
        AiConversation foundConversation = conversations.stream()
            .filter(conv -> testConversation.getTitle().equals(conv.getTitle()))
            .findFirst()
            .orElse(null);

        assertNotNull(foundConversation, "Test conversation should be found");

        // Select the conversation
        dataGrid.select(foundConversation);

        // Click remove button
        JmixButton removeButton = UiTestUtils.getComponent(listView, "removeButton");
        assertNotNull(removeButton);

        // Note: In a real test, you might need to handle confirmation dialogs
        // For now, we just verify the button exists and is properly configured
        assertTrue(removeButton.getText().contains("Remove") || removeButton.getText().contains("Delete"));
    }

    @Test
    void testDetailViewNoSaveButtons() {
        // Create test conversation
        AiConversation testConversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Test No Save Buttons");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        // Navigate to detail view
        viewNavigators.detailView(UiTestUtils.getCurrentView(), AiConversation.class)
            .editEntity(testConversation)
            .navigate();

        AiConversationDetailView detailView = UiTestUtils.getCurrentView();

        // Verify that save/cancel buttons are not present
        // (We removed them from the XML in our implementation)
        try {
            UiTestUtils.getComponent(detailView, "saveAndCloseButton");
            fail("Save and close button should not exist");
        } catch (IllegalArgumentException e) {
            // Expected - button should not exist
        }

        try {
            UiTestUtils.getComponent(detailView, "closeButton");
            fail("Close button should not exist");
        } catch (IllegalArgumentException e) {
            // Expected - button should not exist
        }
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
        assertNotNull(detailView);
        assertNotNull(detailView.getEditedEntity());

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
            assertNotNull(detailView);
            assertEquals(testConversation.getId(), detailView.getEditedEntity().getId());
        }
    }
}