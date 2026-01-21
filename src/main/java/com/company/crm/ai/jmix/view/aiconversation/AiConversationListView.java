package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.action.list.CreateAction;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

@Route(value = "ai-conversations", layout = MainView.class)
@ViewController(id = "AiConversation.list")
@ViewDescriptor(path = "ai-conversation-list-view.xml")
@LookupComponent("aiConversationsDataGrid")
@DialogMode(width = "64em")
public class AiConversationListView extends StandardListView<AiConversation> {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private ViewNavigators viewNavigators;

    @Subscribe("aiConversationsDataGrid.createAction")
    public void onAiConversationsDataGridCreateAction(ActionPerformedEvent event) {
        createNewChatAndNavigate();
    }

    private void createNewChatAndNavigate() {
        // Create and immediately persist a new AI conversation
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle("New AI Chat - " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        conversation.setCreatedDate(OffsetDateTime.now());

        // Save the conversation
        AiConversation savedConversation = dataManager.save(conversation);

        // Navigate to the detail view with the saved conversation
        viewNavigators.detailView(this, AiConversation.class)
                .editEntity(savedConversation)
                .navigate();
    }
}