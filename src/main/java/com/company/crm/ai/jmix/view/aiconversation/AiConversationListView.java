package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "ai-conversations", layout = MainView.class)
@ViewController(id = "AiConversation.list")
@ViewDescriptor(path = "ai-conversation-list-view.xml")
@LookupComponent("aiConversationsDataGrid")
@DialogMode(width = "64em")
public class AiConversationListView extends StandardListView<AiConversation> {

    @Autowired
    private ViewNavigators viewNavigators;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private Messages messages;

    @Subscribe("aiConversationsDataGrid.createAction")
    public void onAiConversationsDataGridCreateAction(ActionPerformedEvent event) {
        createNewChatAndNavigate();
    }

    private void createNewChatAndNavigate() {
        String welcomeMessage = messages.getMessage("aiConversation.welcomeMessage");
        AiConversation savedConversation = aiConversationService.createNewConversation(welcomeMessage);

        // Navigate to the detail view with the saved conversation
        viewNavigators.detailView(this, AiConversation.class)
                .editEntity(savedConversation)
                .navigate();
    }
}