package com.company.crm.ai.view.conversation.support;

import com.company.crm.ai.model.ChatMessage;
import com.company.crm.model.user.User;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
public class AiConversationActorNameResolver {

    private final CurrentAuthentication currentAuthentication;

    public AiConversationActorNameResolver(CurrentAuthentication currentAuthentication) {
        this.currentAuthentication = currentAuthentication;
    }

    public String resolve(ChatMessage message, String defaultActorName) {
        UserDetails currentUser = currentAuthentication.getUser();
        String createdBy = message != null ? message.getCreatedBy() : null;

        if (isCurrentUser(createdBy, currentUser)) {
            return currentUserDisplayName(currentUser, defaultActorName);
        }
        return createdBy;
    }

    private boolean isCurrentUser(String createdBy, UserDetails currentUser) {
        return !StringUtils.hasText(createdBy)
                || Objects.equals(createdBy, currentUser.getUsername());
    }

    private String currentUserDisplayName(UserDetails currentUser, String defaultActorName) {
        if (currentUser instanceof User crmUser && StringUtils.hasText(crmUser.getFullName())) {
            return crmUser.getFullName();
        }
        if (StringUtils.hasText(currentUser.getUsername())) {
            return currentUser.getUsername();
        }
        return defaultActorName;
    }
}
