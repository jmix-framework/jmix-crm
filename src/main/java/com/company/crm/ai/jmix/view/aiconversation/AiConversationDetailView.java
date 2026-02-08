package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.jmix.view.aiconversation.AiConversationFragment;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "ai-conversations/:id", layout = MainView.class)
@ViewController(id = "AiConversation.detail")
@ViewDescriptor(path = "ai-conversation-detail-view.xml")
@EditedEntityContainer("aiConversationDc")
public class AiConversationDetailView extends StandardDetailView<AiConversation> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationDetailView.class);

    @ViewComponent
    private AiConversationFragment aiConversationFragment;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Subscribe
    public void onReady(ReadyEvent event) {
        setupConversationFragment();
    }

    private void setupConversationFragment() {
        // Set user name from current authentication
        if (currentAuthentication.getUser() instanceof com.company.crm.model.user.User) {
            String userName = ((com.company.crm.model.user.User) currentAuthentication.getUser()).getUsername();
            aiConversationFragment.setUserName(userName);
        }

        // Focus the fragment
        aiConversationFragment.focus();

        log.info("AI conversation fragment setup complete");
    }
}