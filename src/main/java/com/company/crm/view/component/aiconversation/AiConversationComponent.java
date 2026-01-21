package com.company.crm.view.component.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import io.jmix.flowui.model.InstallSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reusable AI conversation component that provides chat interface functionality.
 * Can be configured for different use cases (popover chat, detail view, etc.).
 *
 * Supports @Install annotation with "messageProcessor" subject for declarative configuration.
 */
@InstallSubject("messageProcessor")
public class AiConversationComponent extends Composite<VerticalLayout>
        implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AiConversationComponent.class);

    // Config removed - was unnecessarily complex for demonstration purposes

    private MessageList messageList;
    private MessageInput messageInput;
    private ProgressBar progressBar;
    private VerticalLayout headerLayout;


    private String conversationId;
    private List<Consumer<String>> messageSentHandlers = new ArrayList<>();

    // Component state

    // @Install support - Function-based callback pattern following Jmix conventions
    private Function<String, String> messageProcessorProvider;
    private Function<String, List<MessageListItem>> historyLoaderProvider;


    @Override
    @SuppressWarnings("unused") // Called by Spring framework
    public void afterPropertiesSet() {
        log.info("Initializing AiConversationComponent");
        try {
            initComponent();
            log.info("AiConversationComponent initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing component", e);
            throw e;
        }
    }

    private void initComponent() {
        VerticalLayout layout = getContent();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setSizeFull();

        headerLayout = new VerticalLayout();
        headerLayout.setSpacing(false);
        headerLayout.setPadding(false);
        headerLayout.setVisible(false);
        layout.add(headerLayout);

        messageList = new MessageList();
        messageList.setSizeFull();
        messageList.setMarkdown(true);
        layout.add(messageList);

        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        layout.add(progressBar);

        messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onMessageSubmit);
        layout.add(messageInput);

        layout.setFlexGrow(1, messageList);
    }


    /**
     * Sets the conversation ID for this component.
     *
     * @param conversationId the conversation ID
     */
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * Gets the current conversation ID.
     *
     * @return the conversation ID, or null if no conversation has been created yet
     */
    public String getConversationId() {
        return conversationId;
    }


    /**
     * Adds a message sent handler. Framework prefers this method for @Install pattern.
     *
     * @param handler the handler to add
     */
    public void addMessageSentHandler(Consumer<String> handler) {
        if (handler != null) {
            this.messageSentHandlers.add(handler);
        }
    }

    /**
     * Sets a single message sent handler, replacing all existing handlers.
     * Used as fallback by framework for @Install pattern.
     *
     * @param handler the handler to set
     */
    public void setMessageSentHandler(Consumer<String> handler) {
        this.messageSentHandlers.clear();
        if (handler != null) {
            this.messageSentHandlers.add(handler);
        }
    }

    // Configuration methods removed - unnecessarily complex for annotation demo


    /**
     * Focuses the message input field.
     */
    public void focus() {
        if (messageInput != null) {
            messageInput.focus();
        }
    }

    /**
     * Loads conversation history using the configured history loader.
     */
    public void loadHistory() {
        if (historyLoaderProvider != null && conversationId != null) {
            List<MessageListItem> history = historyLoaderProvider.apply(conversationId);
            for (MessageListItem item : history) {
                messageList.addItem(item);
            }
        }
    }

    /**
     * Adds a message to the conversation.
     *
     * @param content    the message content
     * @param username   the username
     * @param colorIndex the color index for the message
     */
    public void addMessage(String content, String username, int colorIndex) {
        MessageListItem item = new MessageListItem(content, username);
        item.setUserColorIndex(colorIndex);
        messageList.addItem(item);
    }

    /**
     * Clears all messages from the conversation.
     */
    public void clearMessages() {
        messageList.setItems();
    }

    /**
     * Shows the progress bar.
     */
    public void showProgress() {
        progressBar.setVisible(true);
    }

    /**
     * Hides the progress bar.
     */
    public void hideProgress() {
        progressBar.setVisible(false);
    }

    public void setEnabled(boolean enabled) {
        getContent().setEnabled(enabled);
        if (messageInput != null) {
            messageInput.setEnabled(enabled);
        }
    }


    private void onMessageSubmit(MessageInput.SubmitEvent event) {
        String userMessage = event.getValue();
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }

        // Fire all message sent handlers
        for (Consumer<String> handler : messageSentHandlers) {
            try {
                handler.accept(userMessage);
            } catch (Exception e) {
                log.warn("Message sent handler failed", e);
            }
        }

        // Send the message
        sendMessage(userMessage);
    }

    private void sendMessage(String userText) {
        log.info("Sending message: {}", userText);

        // Fire message sent handlers
        for (Consumer<String> handler : messageSentHandlers) {
            try {
                handler.accept(userText);
            } catch (Exception e) {
                log.warn("Message sent handler failed", e);
            }
        }

        MessageListItem userItem = new MessageListItem(userText, "User");
        userItem.setUserColorIndex(1);
        messageList.addItem(userItem);

        showProgress();

        messageInput.setEnabled(false);

        // Use the function-based processor for @Install pattern support
        // Note: Async processing should be handled in the @Install method itself
        if (messageProcessorProvider == null) {
            throw new IllegalStateException("messageProcessorProvider is required");
        }

        try {
            String response = messageProcessorProvider.apply(userText);
            // Only handle response if it's not null (allows async processing in @Install method)
            if (response != null) {
                handleAiResponse(response);
            }
            // If response is null, the @Install method handles async processing and UI updates itself
            // The progress bar and input state should remain as set by showProgress() and setEnabled(false)
        } catch (Exception e) {
            handleAiError(e);
        }
    }

    private void handleAiResponse(String response) {
        log.info("Handling AI response: {}", response);

        try {
            handleAssistantMessage(response);
        } catch (Exception e) {
            log.error("Error handling AI response", e);
            handleAiError(e);
        }
    }

    private void handleAiError(Throwable throwable) {
        log.error("Handling AI error: {}", throwable.getMessage(), throwable);

        handleAssistantMessage("Sorry, I'm having trouble connecting right now. Please try again later.");

    }

    private void handleAssistantMessage(String text) {
        hideProgress();

        messageInput.setEnabled(true);

        MessageListItem errorItem = new MessageListItem(
                text,
                "Assistant"
        );
        errorItem.setUserColorIndex(2);
        messageList.addItem(errorItem);
    }


    // Direct configuration methods (called by ComponentLoader and Views)

    /**
     * Sets whether the header should be visible.
     */
    public void setHeaderVisible(boolean visible) {
        headerLayout.setVisible(visible);
    }

    /**
     * Sets the header title.
     */
    public void setHeaderTitle(String title) {
        if (title != null) {
            setupHeaderWithTitle(title);
        }
    }


    /**
     * Adds a welcome message to the conversation.
     */
    @SuppressWarnings("unused") // Used by views and configuration
    public void addWelcomeMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            MessageListItem welcomeItem = new MessageListItem(message, "Assistant");
            welcomeItem.setUserColorIndex(2);
            messageList.addItem(welcomeItem);
        }
    }


    /**
     * Sets the message processor for @Install annotation support.
     * This method enables declarative configuration via @Install(subject = "messageProcessor").
     * The framework automatically detects this setter and calls it with the @Install method wrapped as Function.
     *
     * @param messageProcessor Function that processes user messages and returns AI responses
     */
    @SuppressWarnings("unused") // Called by Jmix framework via @Install pattern
    public void setMessageProcessor(Function<String, String> messageProcessor) {
        this.messageProcessorProvider = messageProcessor;
    }

    /**
     * Sets the history loader using Function-based approach.
     * This replaces the old ConversationHistoryLoader interface with a simpler Function.
     *
     * @param historyLoader Function that takes conversation ID and returns list of MessageListItems
     */
    @SuppressWarnings("unused") // Used by views for manual configuration
    public void setHistoryLoader(Function<String, List<MessageListItem>> historyLoader) {
        this.historyLoaderProvider = historyLoader;
    }

    /**
     * Gets the message list for accessing current messages.
     * @return the message list component
     */
    @SuppressWarnings("unused") // Used by tests and potentially by framework
    public MessageList getMessageList() {
        return messageList;
    }

    /**
     * Gets the message input component for testing purposes.
     * @return the message input component
     */
    @SuppressWarnings("unused") // Used by tests and async handlers
    public MessageInput getMessageInput() {
        return messageInput;
    }

    /**
     * Programmatically sends a message through the component.
     * This is the same as a user typing and submitting a message.
     * Useful for testing purposes.
     *
     * @param message the message to send
     */
    @SuppressWarnings("unused") // Used by tests
    public void submitMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        sendMessage(message);
    }


    private void setupHeaderWithTitle(String title) {
        headerLayout.removeAll();
        headerLayout.setVisible(true);

        HorizontalLayout headerContent = new HorizontalLayout();
        headerContent.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        headerContent.setWidthFull();
        headerContent.setPadding(true);

        H3 titleComponent = new H3(title);
        titleComponent.getStyle().set("margin", "0");
        headerContent.add(titleComponent);

        headerLayout.add(headerContent);
    }
}