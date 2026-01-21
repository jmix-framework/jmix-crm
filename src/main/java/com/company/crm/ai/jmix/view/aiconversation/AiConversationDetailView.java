package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.jmix.memory.JmixChatMemoryRepository;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.view.component.aiconversation.AiConversationComponent;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "ai-conversations/:id", layout = MainView.class)
@ViewController(id = "AiConversation.detail")
@ViewDescriptor(path = "ai-conversation-detail-view.xml")
@EditedEntityContainer("aiConversationDc")
public class AiConversationDetailView extends StandardDetailView<AiConversation> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationDetailView.class);

    @ViewComponent
    AiConversationComponent aiComponent; // package-private for testing

    @Autowired
    private JmixChatMemoryRepository chatMemoryRepository;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private CrmAnalyticsService crmAnalyticsService;

    @Autowired
    private UiAsyncTasks uiAsyncTasks;

    @Autowired
    private DataManager dataManager;


    /**
     * @Install for messageProcessor - starts async processing and returns immediately
     * The actual AI response will be added to the component once the async processing completes
     */
    @Install(to = "aiComponent", subject = "messageProcessor")
    private String processMessage(String userMessage) {
        log.info("Processing message via @Install with CrmAnalyticsService: {}", userMessage);

        // Start async processing
        String conversationId = getEditedEntity().getId().toString();
        uiAsyncTasks.supplierConfigurer(() -> {
            try {
                // Use the real CRM analytics service to process the message
                String response = crmAnalyticsService.processBusinessQuestion(userMessage, conversationId);
                log.info("AI response generated via @Install pattern: {}", response);
                return response;
            } catch (Exception e) {
                log.error("Error processing message via CrmAnalyticsService", e);
                return "I'm sorry, I encountered an error while processing your request: " + e.getMessage() +
                       "\n\nPlease try rephrasing your question or contact support if the problem persists.";
            }
        })
        .withResultHandler(response -> {
            // Ensure UI updates happen on the UI thread
            UI.getCurrent().access(() -> {
                log.info("Adding AI response to UI: {}", response);
                // Add the real AI response
                aiComponent.addMessage(response, "Assistant", 2);
                aiComponent.hideProgress();
                aiComponent.getMessageInput().setEnabled(true);
            });
        })
        .withExceptionHandler(e -> {
            log.error("Async processing failed", e);
            UI.getCurrent().access(() -> {
                log.error("Adding error message to UI");
                aiComponent.addMessage("I'm sorry, something went wrong while processing your request.", "Assistant", 2);
                aiComponent.hideProgress();
                aiComponent.getMessageInput().setEnabled(true);
            });
        })
        .supplyAsync();

        // Return a placeholder that will be replaced by the real response
        return null; // Don't add any immediate response, let async handler do it
    }

    /**
     * History loader implementation using Function-based approach
     */
    private List<MessageListItem> loadChatHistory(String conversationId) {
        List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

        return messages.stream()
                .map(this::createMessageItem)
                .toList();
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        setupConversationComponent();
    }

    private void setupConversationComponent() {
        // Set conversation ID and history loader (Function-based approach)
        aiComponent.setConversationId(getEditedEntity().getId().toString());
        aiComponent.setHistoryLoader(this::loadChatHistory);

        // Add message sent handler
        aiComponent.addMessageSentHandler(this::handleDetailViewMessage);

        // Load existing conversation history
        aiComponent.loadHistory();

        log.info("AI conversation component setup complete with @Install messageProcessor and Function-based historyLoader");
    }

    private void handleDetailViewMessage(String message) {
        // No special handling needed for detail view messages
        // The component handles the AI interaction
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

}