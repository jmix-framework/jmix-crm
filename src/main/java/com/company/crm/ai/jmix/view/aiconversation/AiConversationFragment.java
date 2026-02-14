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
import io.jmix.core.MetadataTools;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.List;


/**
 * Fragment for AI conversation interface that provides chat functionality.
 * Uses provided data containers from the host view for data binding.
 */
@FragmentDescriptor("ai-conversation-fragment.xml")
public class AiConversationFragment extends Fragment<VerticalLayout> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationFragment.class);

    private MessageList messageList;
    private MessageInput messageInput;
    private ProgressBar progressBar;

    @ViewComponent
    private VerticalLayout mainLayout;
    @ViewComponent
    private InstanceContainer<AiConversation> aiConversationDc;

    @Autowired
    private CrmAnalyticsService crmAnalyticsService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;
    @Autowired
    private UiComponents uiComponents;
    @ViewComponent
    private MessageBundle messageBundle;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private MetadataTools metadataTools;


    @Subscribe
    public void onReady(ReadyEvent event) {
        messageList = uiComponents.create(MessageList.class);
        messageList.setSizeFull();
        messageList.setMarkdown(true);

        messageInput = uiComponents.create(MessageInput.class);
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onMessageSubmit);

        progressBar = uiComponents.create(ProgressBar.class);
        progressBar.setWidthFull();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        mainLayout.add(messageList);
        mainLayout.add(progressBar);
        mainLayout.add(messageInput);
        mainLayout.setFlexGrow(1, messageList);

        focusInput();
    }

    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostBeforeShow(View.BeforeShowEvent event) {
        List<MessageListItem> messageListItems = aiConversationDc.getItem().getMessages()
                .stream()
                .map(this::createMessageListItem)
                .toList();

        messageList.setItems(messageListItems);
    }


    /**
     * Converts a ChatMessage entity to a MessageListItem for display.
     */
    private MessageListItem createMessageListItem(ChatMessage message) {
        boolean isAssistant = ChatMessageType.ASSISTANT.equals(message.getType());
        return isAssistant ?
                assistantMessageListItem(message.getContent(), message.getCreatedDate()) :
                userMessageListItem(message.getContent(), message.getCreatedDate());
    }

    /**
     * Creates a MessageListItem for an assistant message with consistent styling.
     */
    private MessageListItem assistantMessageListItem(String content, OffsetDateTime createdAt) {
        MessageListItem item = new MessageListItem(content, createdAt.toInstant(), messageBundle.getMessage("assistantName"));
        item.setUserColorIndex(2);
        return item;
    }

    /**
     * Creates a MessageListItem for a user message with consistent styling.
     */
    private MessageListItem userMessageListItem(String content, OffsetDateTime createdAt) {
        UserDetails user = currentAuthentication.getUser();
        String userName = metadataTools.getInstanceName(user);
        MessageListItem item = new MessageListItem(content, createdAt.toInstant(), userName);
        item.setUserAbbreviation(user.getUsername().substring(0, 1));
        item.setUserColorIndex(1);
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

        MessageListItem userItem = userMessageListItem(userMessage, now());
        messageList.addItem(userItem);

        progressBar.setVisible(true);
        messageInput.setEnabled(false);

        AiConversation conversation = aiConversationDc.getItem();

        uiAsyncTasks.supplierConfigurer(() ->
                    crmAnalyticsService.processBusinessQuestion(userMessage, conversation.getId().toString())
                )
                .withResultHandler(response -> {
                    messageList.addItem(assistantMessageListItem(response, now()));
                    getFragmentData().loadAll();
                    focusInput();
                })
                .withExceptionHandler(e -> {
                    log.error("Error processing message async", e);
                    String errorMessage = "I'm sorry, I encountered an error while processing your request: " + e.getMessage() +
                            "\n\nPlease try rephrasing your question or contact support if the problem persists.";
                    MessageListItem errorItem = assistantMessageListItem(errorMessage, now());
                    messageList.addItem(errorItem);
                    focusInput();
                })
                .supplyAsync();
    }

    public void focusInput() {
        progressBar.setVisible(false);
        messageInput.setEnabled(true);
        messageInput.focus();
    }

    private OffsetDateTime now() {
        return timeSource.now().toOffsetDateTime();
    }
}