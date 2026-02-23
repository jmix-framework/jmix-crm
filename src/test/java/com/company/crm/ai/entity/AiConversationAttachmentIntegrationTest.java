package com.company.crm.ai.entity;

import com.company.crm.AbstractTest;
import com.company.crm.ai.service.AiConversationService;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FetchPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiConversationAttachmentIntegrationTest extends AbstractTest {

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private DataManager dataManager;

    @Test
    void testSaveAndLoadAttachment() {
        // 1. Create a conversation
        AiConversation conversation = aiConversationService.createNewConversation("Test Conversation");

        // 2. Create an attachment
        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setConversation(conversation);
        attachment.setFileName("test-report.html");
        attachment.setTitle("Test Report Title");
        attachment.setType(AiAttachmentType.AI_GENERATED);
        
        FileRef fileRef = new FileRef("storage", "2026/02/22/test-report.html", "test-report.html");
        attachment.setFile(fileRef);

        // 3. Save
        dataManager.save(attachment);

        // 4. Verify persistence and relationship
        AiConversation reloadedConversation = dataManager.load(AiConversation.class)
                .id(conversation.getId())
                .fetchPlan(fp -> {
                    fp.add("attachments", sub -> sub.addFetchPlan(FetchPlan.BASE));
                })
                .one();

        assertThat(reloadedConversation.getAttachments()).hasSize(1);
        AiConversationAttachment reloadedAttachment = reloadedConversation.getAttachments().get(0);
        
        assertThat(reloadedAttachment.getFileName()).isEqualTo("test-report.html");
        assertThat(reloadedAttachment.getTitle()).isEqualTo("Test Report Title");
        assertThat(reloadedAttachment.getType()).isEqualTo(AiAttachmentType.AI_GENERATED);
        assertThat(reloadedAttachment.getFile()).isEqualTo(fileRef);
        assertThat(reloadedAttachment.getConversation().getId()).isEqualTo(conversation.getId());
    }

    @Test
    void testSaveAttachment_withoutFile_fails() {
        AiConversation conversation = aiConversationService.createNewConversation("Attachment Validation File");

        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setConversation(conversation);
        attachment.setFileName("missing-file.html");
        attachment.setType(AiAttachmentType.AI_GENERATED);

        assertThatThrownBy(() -> dataManager.save(attachment))
                .isInstanceOf(Exception.class);
    }

    @Test
    void testSaveAttachment_withoutFileName_fails() {
        AiConversation conversation = aiConversationService.createNewConversation("Attachment Validation FileName");

        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setConversation(conversation);
        attachment.setFile(new FileRef("storage", "2026/02/22/file-only.html", "file-only.html"));
        attachment.setType(AiAttachmentType.AI_GENERATED);

        assertThatThrownBy(() -> dataManager.save(attachment))
                .isInstanceOf(Exception.class);
    }
}
