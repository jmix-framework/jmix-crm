package com.company.crm.test.ai.model;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FileRef;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

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
        AiConversation conversation = aiConversationService.createNewConversation();
        ChatMessage message = createMessage(conversation);
        FileRef fileRef = new FileRef("storage", "2026/02/22/test-report.html", "test-report.html");
        AiConversationAttachment attachment = createAttachment(
                message,
                "test-report.html",
                fileRef,
                "Test Report Title",
                AiAttachmentOrigin.AI_GENERATED
        );

        // when
        dataManager.save(attachment);

        ChatMessage reloadedMessage = dataManager.load(ChatMessage.class)
                .id(message.getId())
                .fetchPlan(fp -> {
                    fp.add("attachments", sub -> sub.addFetchPlan(FetchPlan.BASE));
                })
                .one();

        // then
        assertThat(reloadedMessage.getAttachments()).hasSize(1);
        AiConversationAttachment reloadedAttachment = reloadedMessage.getAttachments().getFirst();

        assertThat(reloadedAttachment.getFileName()).isEqualTo("test-report.html");
        assertThat(reloadedAttachment.getTitle()).isEqualTo("Test Report Title");
        assertThat(reloadedAttachment.getOrigin()).isEqualTo(AiAttachmentOrigin.AI_GENERATED);
        assertThat(reloadedAttachment.getFile()).isEqualTo(fileRef);
        assertThat(reloadedAttachment.getMessage().getId()).isEqualTo(message.getId());
    }

    @Test
    void testSaveAttachment_withoutFile_fails() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation();
        ChatMessage message = createMessage(conversation);
        AiConversationAttachment attachment = createAttachment(
                message,
                "missing-file.html",
                null,
                "Missing File",
                AiAttachmentOrigin.AI_GENERATED
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
        AiConversation conversation = aiConversationService.createNewConversation();
        ChatMessage message = createMessage(conversation);
        AiConversationAttachment attachment = createAttachment(
                message,
                null,
                new FileRef("storage", "2026/02/22/file-only.html", "file-only.html"),
                "Missing FileName",
                AiAttachmentOrigin.AI_GENERATED
        );

        // when / then
        assertThatThrownBy(() -> dataManager.save(attachment))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("constraints were violated")
                .hasMessageContaining("AiConversationAttachment");
    }

    private ChatMessage createMessage(AiConversation conversation) {
        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(conversation);
        message.setType(ChatMessageType.ASSISTANT);
        message.setCreatedDate(OffsetDateTime.now());
        return dataManager.save(message);
    }

    private AiConversationAttachment createAttachment(ChatMessage message,
                                                      String fileName,
                                                      FileRef fileRef,
                                                      String title,
                                                      AiAttachmentOrigin origin) {
        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setMessage(message);
        attachment.setFileName(fileName);
        attachment.setFile(fileRef);
        attachment.setTitle(title);
        attachment.setOrigin(origin);
        return attachment;
    }
}
