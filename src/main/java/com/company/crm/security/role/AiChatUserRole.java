package com.company.crm.security.role;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

@RowLevelRole(name = "AI Chat User Row-level", code = AiChatUserRole.CODE)
public interface AiChatUserRole {
    String CODE = "ai-chat-user-rl";

    @JpqlRowLevelPolicy(entityClass = AiConversation.class, where = "{E}.createdBy = :current_user_username")
    void aiConversation();

    @JpqlRowLevelPolicy(entityClass = ChatMessage.class, where = "{E}.createdBy = :current_user_username")
    void chatMessage();
}
