package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.jmix.memory.JmixChatMemoryRepository;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "ai-conversations/:id", layout = MainView.class)
@ViewController(id = "AiConversation.detail")
@ViewDescriptor(path = "ai-conversation-detail-view.xml")
@EditedEntityContainer("aiConversationDc")
public class AiConversationDetailView extends StandardDetailView<AiConversation> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationDetailView.class);

    @ViewComponent
    private VerticalLayout messageList;

    @ViewComponent
    private VerticalLayout messageInputContainer;

    @ViewComponent
    private ProgressBar aiProcessingProgress;

    // Vaadin components created programmatically
    private MessageList vaadinMessageList;
    private MessageInput messageInput;

    @Autowired
    private JmixChatMemoryRepository chatMemoryRepository;

    @Autowired
    private CrmAnalyticsService analyticsService;

    @Autowired
    private UiAsyncTasks uiAsyncTasks;


    @Subscribe
    public void onReady(ReadyEvent event) {
        setupMessageComponents();
        loadChatHistory();
    }

    private void setupMessageComponents() {
        // Create Vaadin MessageList
        vaadinMessageList = new MessageList();
        vaadinMessageList.setSizeFull();
        vaadinMessageList.setMarkdown(true);
        messageList.add(vaadinMessageList);

        // Create Vaadin MessageInput
        messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInputContainer.add(messageInput);

        // Add submit listener
        messageInput.addSubmitListener(e -> {
            String userMessage = e.getValue();
            if (userMessage != null && !userMessage.trim().isEmpty()) {
                sendMessage(userMessage);
            }
        });
    }

    private void loadChatHistory() {

        String conversationId = getEditedEntity().getId().toString();
        List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

        List<MessageListItem> items = messages.stream()
                .map(this::createMessageItem)
                .toList();

        vaadinMessageList.setItems(items);

    }

    private MessageListItem createMessageItem(Message message) {
        boolean isAssistant = message instanceof AssistantMessage;
        MessageListItem item = new MessageListItem(
                message.getText(),
                isAssistant ? "Assistant" : "User"
        );
        item.setUserColorIndex(isAssistant ? 2 : 1);
        return item;
    }

    private void sendMessage(String userText) {
        // MessageInput clears automatically after submit
        // Add user message to the list
        MessageListItem userItem = createMessageItem(new UserMessage(userText));
        vaadinMessageList.addItem(userItem);

        // Add empty Assistant message that will be updated with streaming response
        MessageListItem assistantItem = createMessageItem(new AssistantMessage(""));
        vaadinMessageList.addItem(assistantItem);

        // Show progress bar
        aiProcessingProgress.setVisible(true);

        // Process AI response in a separate thread
        String conversationId = getEditedEntity().getId().toString();

        // Get AI response asynchronously using UiAsyncTasks with conversation memory
        // The service will handle loading conversation history and saving new messages
        uiAsyncTasks.supplierConfigurer(() -> analyticsService.processBusinessQuestion(userText, conversationId))
                .withResultHandler(aiResponse -> {
                    // Hide progress bar
                    aiProcessingProgress.setVisible(false);

                    // Update the Assistant message with the response
                    assistantItem.setText(aiResponse);

                    // Memory is automatically handled by Spring AI ChatMemoryAdvisor

                })
                .withExceptionHandler(throwable -> {
                    // Hide progress bar
                    aiProcessingProgress.setVisible(false);

                    // Handle error
                    log.error("Error in AI response processing", throwable);
                    assistantItem.setText("Sorry, I encountered an error processing your message.");
                })
                .supplyAsync();
    }


}