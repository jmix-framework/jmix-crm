package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.jmix.memory.JmixChatMemoryRepository;
import com.company.crm.app.service.ai.CrmAnalyticsAsyncLoader;
import com.company.crm.view.component.aiconversation.AiConversationComponent;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
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
    private CrmAnalyticsAsyncLoader crmAnalyticsAsyncLoader;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuthentication;


    /**
     * @Install for messageProcessor - starts async processing and returns immediately
     * The actual AI response will be added to the component once the async processing completes
     */
    @Install(to = "aiComponent", subject = "messageProcessor")
    private String processMessage(String userMessage) {
        String conversationId = getEditedEntity().getId().toString();
        crmAnalyticsAsyncLoader.processMessageAsync(userMessage, conversationId, aiComponent);

        // Return null - async processing will handle UI updates
        return null;
    }

    /**
     * @Install for historyLoader - loads conversation history from the memory repository
     */
    @Install(to = "aiComponent", subject = "historyLoader")
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
        // Set conversation ID - historyLoader is now handled via @Install annotation
        aiComponent.setConversationId(getEditedEntity().getId().toString());

        // Set current username for user messages
        aiComponent.setUserName(((com.company.crm.model.user.User) currentAuthentication.getUser()).getUsername());

        // No additional message sent handler needed - component handles everything

        // Load existing conversation history
        aiComponent.loadHistory();

        log.info("AI conversation component setup complete with @Install messageProcessor and @Install historyLoader");
    }


    private MessageListItem createMessageItem(Message message) {
        boolean isAssistant = message instanceof AssistantMessage;
        MessageListItem item = new MessageListItem(
                message.getText(),
                isAssistant ? aiComponent.getAssistantName() : aiComponent.getUserName()
        );
        item.setUserColorIndex(isAssistant ? 2 : 1);
        return item;
    }

}