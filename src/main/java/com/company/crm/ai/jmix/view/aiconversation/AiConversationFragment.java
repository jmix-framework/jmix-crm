package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.UiComponents;
import io.jmix.core.DataManager;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fragment for AI conversation interface that provides chat functionality.
 * Has its own data loading logic and works with conversationId parameter.
 */
@FragmentDescriptor("ai-conversation-fragment.xml")
public class AiConversationFragment extends Fragment<VerticalLayout> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationFragment.class);

    private MessageList messageList;
    private MessageInput messageInput;

    @ViewComponent
    private ProgressBar progressBar;

    @ViewComponent
    private VerticalLayout mainLayout;

    @Autowired
    private CrmAnalyticsService crmAnalyticsService;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private UiAsyncTasks uiAsyncTasks;

    @Autowired
    private UiComponents uiComponents;

    private String assistantName = "Assistant";
    private String userName = "User";
    private UUID conversationId;

    /**
     * Sets the conversation ID for this fragment.
     */
    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }


    @Subscribe
    public void onReady(ReadyEvent event) {
        log.info("AiConversationFragment ready - initializing component");
        initializeFragment();
    }

    private void initializeFragment() {
        // Create MessageList programmatically
        messageList = uiComponents.create(MessageList.class);
        messageList.setSizeFull();
        messageList.setMarkdown(true);

        // Create MessageInput programmatically
        messageInput = uiComponents.create(MessageInput.class);
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onMessageSubmit);

        // Configure ProgressBar (already in XML)
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        // Add components to layout in correct order: MessageList, ProgressBar, MessageInput
        mainLayout.remove(progressBar); // Remove from default position
        mainLayout.add(messageList);
        mainLayout.add(progressBar); // Add between list and input
        mainLayout.add(messageInput);
        mainLayout.setFlexGrow(1, messageList);

        // Set user name from current authentication
        if (currentAuthentication.getUser() instanceof com.company.crm.model.user.User) {
            userName = ((com.company.crm.model.user.User) currentAuthentication.getUser()).getUsername();
        }

        // Load conversation data if conversationId is set
        if (conversationId != null) {
            loadConversationData();
        }
    }

    /**
     * Loads conversation data from database using DataManager.
     */
    private void loadConversationData() {
        log.info("Loading conversation data for ID: {}", conversationId);

        AiConversation conversation = dataManager.load(AiConversation.class)
                .id(conversationId)
                .fetchPlan(builder -> builder.add("_base").add("messages", "_base"))
                .optional()
                .orElse(null);

        if (conversation != null) {
            updateMessageList(conversation);
        } else {
            log.warn("Conversation not found: {}", conversationId);
        }
    }

    /**
     * Updates the MessageList with messages from the conversation.
     */
    private void updateMessageList(AiConversation conversation) {
        if (messageList == null) {
            return;
        }

        messageList.setItems(); // Clear existing items

        if (conversation.getMessages() != null) {
            // Messages are already sorted by createdDate via @OrderBy annotation on the entity
            for (ChatMessage message : conversation.getMessages()) {
                MessageListItem item = createMessageListItem(message);
                messageList.addItem(item);
            }
        }

        log.info("Updated MessageList with {} messages",
                conversation.getMessages() != null ? conversation.getMessages().size() : 0);
    }


    /**
     * Converts a ChatMessage entity to a MessageListItem for display.
     */
    private MessageListItem createMessageListItem(ChatMessage message) {
        boolean isAssistant = ChatMessageType.ASSISTANT.equals(message.getType());
        String displayName = isAssistant ? assistantName : userName;

        MessageListItem item = new MessageListItem(message.getContent(), displayName);
        item.setUserColorIndex(isAssistant ? 2 : 1);

        return item;
    }

    /**
     * Handles message submission from the MessageInput component.
     */
    private void onMessageSubmit(MessageInput.SubmitEvent event) {
        String userMessage = event.getValue();
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }

        log.info("Processing user message: {}", userMessage);

        // Add user message to UI immediately
        MessageListItem userItem = new MessageListItem(userMessage, userName);
        userItem.setUserColorIndex(1);
        messageList.addItem(userItem);

        // User message will be saved by Spring AI ChatMemory in CrmAnalyticsService

        // Show progress and disable input
        showProgress();
        messageInput.setEnabled(false);

        // Process message asynchronously
        processMessageAsync(userMessage);
    }

    /**
     * Processes the user message asynchronously and handles the response.
     */
    private void processMessageAsync(String userMessage) {
        String conversationIdStr = conversationId.toString();

        uiAsyncTasks.supplierConfigurer(() -> {
                    try {
                        return crmAnalyticsService.processBusinessQuestion(userMessage, conversationIdStr);
                    } catch (Exception e) {
                        log.error("Error processing message async", e);
                        return "I'm sorry, I encountered an error while processing your request: " + e.getMessage() +
                                "\n\nPlease try rephrasing your question or contact support if the problem persists.";
                    }
                })
                .withResultHandler(response -> {
                    // Add assistant response to UI
                    MessageListItem assistantItem = new MessageListItem(response, assistantName);
                    assistantItem.setUserColorIndex(2);
                    messageList.addItem(assistantItem);

                    // Reload conversation data to get the latest persisted messages
                    loadConversationData();

                    // Hide progress, enable input and focus for immediate typing
                    hideProgress();
                    messageInput.setEnabled(true);
                    messageInput.focus();

                    log.info("Assistant response processed and displayed");
                })
                .withExceptionHandler(e -> {
                    log.error("Async processing failed", e);

                    // Add error message to UI
                    MessageListItem errorItem = new MessageListItem(
                            "I'm sorry, something went wrong while processing your request.",
                            assistantName
                    );
                    errorItem.setUserColorIndex(2);
                    messageList.addItem(errorItem);

                    // Hide progress, enable input and focus for retry
                    hideProgress();
                    messageInput.setEnabled(true);
                    messageInput.focus();
                })
                .supplyAsync();
    }

    /**
     * Saves a message via the Spring AI ChatMemory (not directly to DataContext).
     * The message will be persisted by the ChatMemory and can be loaded later.
     */
    private void saveMessage(String content, ChatMessageType type) {
        // Message is already saved by Spring AI ChatMemory in CrmAnalyticsService
        // We don't need to manually save it here to avoid Jmix "unsaved changes" warnings
        log.info("Message saved via Spring AI ChatMemory: {}", type);
    }

    /**
     * Shows the progress indicator.
     */
    public void showProgress() {
        progressBar.setVisible(true);
    }

    /**
     * Hides the progress indicator.
     */
    public void hideProgress() {
        progressBar.setVisible(false);
    }

    /**
     * Sets the assistant name for display in messages.
     */
    public void setAssistantName(String assistantName) {
        this.assistantName = assistantName != null ? assistantName : "Assistant";
    }

    /**
     * Gets the assistant name.
     */
    public String getAssistantName() {
        return assistantName;
    }

    /**
     * Sets the user name for display in messages.
     */
    public void setUserName(String userName) {
        this.userName = userName != null ? userName : "User";
    }

    /**
     * Gets the user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Focuses the message input field.
     */
    public void focus() {
        if (messageInput != null) {
            messageInput.focus();
        }
    }

    /**
     * Clears all messages from the UI (does not affect database).
     */
    public void clearMessages() {
        messageList.setItems();
    }

    /**
     * Refreshes the conversation history by reloading data from database.
     */
    public void refreshHistory() {
        loadConversationData();
    }

    /**
     * Gets the MessageList component for testing purposes.
     */
    public MessageList getMessageList() {
        return messageList;
    }

    /**
     * Gets the MessageInput component for testing purposes.
     */
    public MessageInput getMessageInput() {
        return messageInput;
    }

    /**
     * Programmatically submits a message (for testing purposes).
     */
    public void submitMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        MessageInput.SubmitEvent event = new MessageInput.SubmitEvent(messageInput, false, message);
        onMessageSubmit(event);
    }
}