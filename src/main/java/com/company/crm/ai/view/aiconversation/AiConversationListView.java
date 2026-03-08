package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "ai-conversations", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.AI_CONVERSATION_LIST)
@ViewDescriptor(path = "/com/company/crm/ai/view/aiconversation/ai-conversation-list-view.xml")
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
        AiConversation savedConversation = createConversationWithWelcomeMessage();

        viewNavigators.detailView(this, AiConversation.class)
                .editEntity(savedConversation)
                .navigate();
    }

    private AiConversation createConversationWithWelcomeMessage() {
        String welcomeMessage = messages.getMessage("aiConversation.welcomeMessage");
        return aiConversationService.createNewConversation(welcomeMessage);
    }
}
