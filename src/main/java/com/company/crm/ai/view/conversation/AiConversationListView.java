package com.company.crm.ai.view.conversation;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "ai-conversations/all", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.AI_CONVERSATION_LIST)
@ViewDescriptor(path = "ai-conversation-list-view.xml")
@LookupComponent("aiConversationsDataGrid")
@DialogMode(width = "90%", resizable = true)
public class AiConversationListView extends StandardListView<AiConversation> {
}
