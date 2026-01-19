package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@Route(value = "ai-conversations", layout = MainView.class)
@ViewController(id = "AiConversation.list")
@ViewDescriptor(path = "ai-conversation-list-view.xml")
@LookupComponent("aiConversationsDataGrid")
@DialogMode(width = "64em")
public class AiConversationListView extends StandardListView<AiConversation> {
}