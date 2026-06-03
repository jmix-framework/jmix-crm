package com.company.crm.test.ai.security;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.ai.view.context.AiChatAboutThisSupport;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.client.Client;
import com.company.crm.security.role.ManagerRole;
import com.company.crm.security.role.UiMinimalRole;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.AccessManager;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiConversationSecurityTest extends AbstractTest {

    @Autowired
    private AiConversationService aiConversationService;
    @Autowired
    private AiChatAboutThisSupport aiChatAboutThisSupport;
    @Autowired
    private IdSerialization idSerialization;
    @Autowired
    private AccessManager accessManager;

    @Test
    void aiConversationViewPermissionControlsChatAboutThisPermission() {
        var manager = testUsers.ensureUser("ai-context-manager");
        testUsers.assignRole(manager.getUsername(), ManagerRole.CODE);
        var minimal = testUsers.ensureUser("ai-context-minimal");
        testUsers.assignRole(minimal.getUsername(), UiMinimalRole.CODE);

        assertAiViewAccess(manager.getUsername(), true);
        assertAiViewAccess(minimal.getUsername(), false);

        systemAuthenticator.runWithUser(manager.getUsername(),
                () -> assertThat(aiChatAboutThisSupport.isOpenChatPermitted()).isTrue());
        systemAuthenticator.runWithUser(minimal.getUsername(),
                () -> assertThat(aiChatAboutThisSupport.isOpenChatPermitted()).isFalse());
    }

    @Test
    void aiConversationChildrenAreRowLevelIsolatedByOwningConversation() {
        var owner = testUsers.ensureUser("ai-context-owner");
        testUsers.assignRole(owner.getUsername(), ManagerRole.CODE);
        var other = testUsers.ensureUser("ai-context-other");
        testUsers.assignRole(other.getUsername(), ManagerRole.CODE);

        UUID ownerConversationId = createConversationWithChildren(owner.getUsername(), "Owner Client");
        UUID otherConversationId = createConversationWithChildren(other.getUsername(), "Other Client");

        systemAuthenticator.runWithUser(owner.getUsername(), () -> {
            assertVisibleConversationIds(ownerConversationId);
            assertVisibleChildCounts(1);
        });
        systemAuthenticator.runWithUser(other.getUsername(), () -> {
            assertVisibleConversationIds(otherConversationId);
            assertVisibleChildCounts(1);
        });
    }

    private UUID createConversationWithChildren(String username, String clientName) {
        return systemAuthenticator.withUser(username, () -> {
            Client client = entities.client(clientName + " " + UUID.randomUUID());
            AiConversation conversation = aiConversationService.createNewConversation();
            aiConversationService.createUserMessage(
                    conversation,
                    "Analyse secured context",
                    List.of(idSerialization.idToString(Id.of(client))),
                    List.of(new PendingAttachmentInput(fileRef("secured-context.txt"), "secured-context.txt"))
            );
            return conversation.getId();
        });
    }

    private void assertAiViewAccess(String username, boolean expected) {
        systemAuthenticator.runWithUser(username, () -> {
            UiShowViewContext context = new UiShowViewContext(CrmConstants.ViewIds.AI_CONVERSATION_DETAIL);
            accessManager.applyRegisteredConstraints(context);
            assertThat(context.isPermitted()).isEqualTo(expected);
        });
    }

    private void assertVisibleConversationIds(UUID expectedConversationId) {
        assertThat(dataManager.load(AiConversation.class).all().list())
                .extracting(AiConversation::getId)
                .containsExactly(expectedConversationId);
    }

    private void assertVisibleChildCounts(int expectedCount) {
        assertThat(dataManager.load(ChatMessage.class).all().list()).hasSize(expectedCount);
        assertThat(dataManager.load(AiConversationAttachment.class).all().list()).hasSize(expectedCount);
        assertThat(dataManager.load(ChatMessageEntityReference.class).all().list()).hasSize(expectedCount);
    }

    private FileRef fileRef(String fileName) {
        return new FileRef("storage", "test/" + fileName, fileName);
    }
}
