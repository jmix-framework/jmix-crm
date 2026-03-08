package com.company.crm.ai.model;

import com.company.crm.AbstractTest;
import com.company.crm.ai.service.AiConversationService;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FetchPlan;
import jakarta.validation.ConstraintViolationException;
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
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Test Conversation");
        FileRef fileRef = new FileRef("storage", "2026/02/22/test-report.html", "test-report.html");
        AiConversationAttachment attachment = createAttachment(
                conversation,
                "test-report.html",
                fileRef,
                "Test Report Title",
                AiAttachmentType.AI_GENERATED
        );

        // when
        dataManager.save(attachment);

        AiConversation reloadedConversation = dataManager.load(AiConversation.class)
                .id(conversation.getId())
                .fetchPlan(fp -> {
                    fp.add("attachments", sub -> sub.addFetchPlan(FetchPlan.BASE));
                })
                .one();

        // then
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
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Attachment Validation File");
        AiConversationAttachment attachment = createAttachment(
                conversation,
                "missing-file.html",
                null,
                "Missing File",
                AiAttachmentType.AI_GENERATED
        );

        // when / then
        assertThatThrownBy(() -> dataManager.save(attachment))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("constraints were violated")
                .hasMessageContaining("AiConversationAttachment");
    }

    @Test
    void testSaveAttachment_withoutFileName_fails() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Attachment Validation FileName");
        AiConversationAttachment attachment = createAttachment(
                conversation,
                null,
                new FileRef("storage", "2026/02/22/file-only.html", "file-only.html"),
                "Missing FileName",
                AiAttachmentType.AI_GENERATED
        );

        // when / then
        assertThatThrownBy(() -> dataManager.save(attachment))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("constraints were violated")
                .hasMessageContaining("AiConversationAttachment");
    }

    private AiConversationAttachment createAttachment(AiConversation conversation,
                                                      String fileName,
                                                      FileRef fileRef,
                                                      String title,
                                                      AiAttachmentType type) {
        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setConversation(conversation);
        attachment.setFileName(fileName);
        attachment.setFile(fileRef);
        attachment.setTitle(title);
        attachment.setType(type);
        return attachment;
    }
}
