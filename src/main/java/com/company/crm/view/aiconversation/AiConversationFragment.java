package com.company.crm.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Fragment for AI conversation interface that provides chat functionality.
 * Works directly with AiConversation and ChatMessage entities for simplified data handling.
 */
@FragmentDescriptor("ai-conversation-fragment.xml")
public class AiConversationFragment extends Fragment<VerticalLayout> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationFragment.class);

    @ViewComponent
    private MessageList messageList;

    @ViewComponent
    private MessageInput messageInput;

    @ViewComponent
    private ProgressBar progressBar;

    @ViewComponent
    private InstanceContainer<AiConversation> conversationDc;

    @ViewComponent
    private CollectionContainer<ChatMessage> messagesDc;

    @Autowired
    private CrmAnalyticsService crmAnalyticsService;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private UiAsyncTasks uiAsyncTasks;

    private String assistantName = "Assistant";
    private String userName = "User";

    @Subscribe
    public void onReady(ReadyEvent event) {
        log.info("AiConversationFragment ready - initializing component");
        initializeFragment();
    }

    private void initializeFragment() {
        // Configure MessageList
        messageList.setSizeFull();
        messageList.setMarkdown(true);

        // Configure MessageInput
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onMessageSubmit);

        // Configure ProgressBar
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        // Set user name from current authentication
        if (currentAuthentication.getUser() instanceof com.company.crm.model.user.User) {
            userName = ((com.company.crm.model.user.User) currentAuthentication.getUser()).getUsername();
        }

        // Load conversation history
        loadConversationHistory();
    }

    /**
     * Loads conversation history from ChatMessage entities and displays them in the MessageList.
     */
    private void loadConversationHistory() {
        if (conversationDc.getItemOrNull() == null) {
            log.warn("No conversation loaded in conversationDc");
            return;
        }

        AiConversation conversation = conversationDc.getItem();
        log.info("Loading conversation history for conversation ID: {}", conversation.getId());

        // Load messages from database
        List<ChatMessage> messages = dataManager.load(ChatMessage.class)
                .query("select m from ChatMessage m where m.conversation = :conversation order by m.createdDate")
                .parameter("conversation", conversation)
                .list();

        // Convert to MessageListItems and add to MessageList
        messageList.setItems();  // Clear existing items

        for (ChatMessage message : messages) {
            MessageListItem item = createMessageListItem(message);
            messageList.addItem(item);
        }

        log.info("Loaded {} messages for conversation", messages.size());
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

        // Save user message to database
        saveMessage(userMessage, ChatMessageType.USER);

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
        AiConversation conversation = conversationDc.getItem();
        String conversationId = conversation.getId().toString();

        uiAsyncTasks.supplierConfigurer(() -> {
                    try {
                        return crmAnalyticsService.processBusinessQuestion(userMessage, conversationId);
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

                    // Save assistant message to database
                    saveMessage(response, ChatMessageType.ASSISTANT);

                    // Hide progress and enable input
                    hideProgress();
                    messageInput.setEnabled(true);

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

                    // Hide progress and enable input
                    hideProgress();
                    messageInput.setEnabled(true);
                })
                .supplyAsync();
    }

    /**
     * Saves a message to the database as a ChatMessage entity.
     */
    private void saveMessage(String content, ChatMessageType type) {
        AiConversation conversation = conversationDc.getItem();

        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(conversation);
        message.setContent(content);
        message.setType(type);
        message.setCreatedDate(OffsetDateTime.now());
        message.setCreatedBy(currentAuthentication.getUser().getUsername());

        dataManager.save(message);

        log.info("Saved {} message to database", type);
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
     * Refreshes the conversation history from the database.
     */
    public void refreshHistory() {
        loadConversationHistory();
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