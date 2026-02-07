package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "ai-conversations/:id", layout = MainView.class)
@ViewController(id = "AiConversation.detail")
@ViewDescriptor(path = "ai-conversation-detail-view.xml")
@EditedEntityContainer("aiConversationDc")
public class AiConversationDetailView extends StandardDetailView<AiConversation> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationDetailView.class);

    /** Indicates that message processing is handled asynchronously */
    private static final String ASYNC_PROCESSING = null;

    @ViewComponent
    AiConversationComponent aiComponent; // package-private for testing


    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private CrmAnalyticsAsyncLoader crmAnalyticsAsyncLoader;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuthentication;


    /**
     * Handles user messages in the AI conversation interface.
     *
     * <p>This method processes user messages asynchronously to avoid blocking the UI thread
     * during AI query processing. The message is forwarded to the CRM analytics service
     * which will generate a response and update the conversation component automatically.
     *
     * @param userMessage the message entered by the user
     * @return null to indicate async processing (actual response will be added later)
     * @see CrmAnalyticsAsyncLoader#processMessageAsync
     */
    @Install(to = "aiComponent", subject = "messageProcessor")
    private String processMessage(String userMessage) {
        String conversationId = getEditedEntity().getId().toString();
        crmAnalyticsAsyncLoader.processMessageAsync(userMessage, conversationId, aiComponent);

        return null; // Async processing - response will be added by background task
    }

    /**
     * @Install for historyLoader - loads conversation history from the memory repository
     */
    @Install(to = "aiComponent", subject = "historyLoader")
    private List<MessageListItem> loadChatHistory(String conversationId) {
        return crmAnalyticsAsyncLoader.loadConversationHistory(conversationId);
    }

    @Subscribe
    public void onReady(ReadyEvent event) {
        setupConversationComponent();
    }

    private void setupConversationComponent() {
        aiComponent.setConversationId(getEditedEntity().getId().toString());
        aiComponent.setUserName(((com.company.crm.model.user.User) currentAuthentication.getUser()).getUsername());
        aiComponent.loadHistory();

        log.info("AI conversation component setup complete with @Install messageProcessor and @Install historyLoader");
    }
}