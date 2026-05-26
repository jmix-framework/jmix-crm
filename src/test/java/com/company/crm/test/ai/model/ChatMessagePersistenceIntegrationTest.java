package com.company.crm.test.ai.model;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.model.catalog.category.Category;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.SaveContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ChatMessage} and its related entities.
 * Verifies the composition-based persistence of attachments and entity references.
 */
public class ChatMessagePersistenceIntegrationTest extends AbstractTest {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private IdSerialization idSerialization;

    @Test
    void testPersistMessageWithContextCompositions() {
        // given
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> {
            conv.setTitle("Persistence Test");
            conv.setCreatedDate(OffsetDateTime.now());
        });

        Category category = createAndSaveEntity(Category.class, cat -> {
            cat.setName("Test Category");
            cat.setCode("test-cat-" + UUID.randomUUID());
        });
        String entityRef = idSerialization.idToString(Id.of(category));

        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(conversation);
        message.setType(ChatMessageType.USER);
        message.setContent("Message with context");
        message.setCreatedDate(OffsetDateTime.now());

        List<ChatMessageEntityReference> entityReferences = new ArrayList<>();
        ChatMessageEntityReference ref = dataManager.create(ChatMessageEntityReference.class);
        ref.setMessage(message);
        ref.setEntityReference(entityRef);
        ref.setSortOrder(0);
        entityReferences.add(ref);

        List<AiConversationAttachment> attachments = new ArrayList<>();
        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setMessage(message);
        attachment.setFileName("test.txt");
        attachment.setTitle("Test Attachment");
        attachment.setFile(new FileRef("storage", "test.txt", "test.txt"));
        attachment.setOrigin(AiAttachmentOrigin.USER_UPLOADED);
        attachments.add(attachment);

        message.setEntityReferences(entityReferences);
        message.setAttachments(attachments);

        // when - using composition save context
        SaveContext saveContext = new SaveContext().saving(message, ref, attachment);
        dataManager.save(saveContext);

        // then
        ChatMessage loadedMessage = dataManager.load(ChatMessage.class)
                .id(message.getId())
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                        .add("entityReferences", FetchPlan.BASE)
                        .add("attachments", FetchPlan.BASE))
                .one();

        assertThat(loadedMessage.getEntityReferences()).hasSize(1);
        assertThat(loadedMessage.getEntityReferences().get(0).getEntityReference()).isEqualTo(entityRef);
        assertThat(loadedMessage.getAttachments()).hasSize(1);
        assertThat(loadedMessage.getAttachments().get(0).getFileName()).isEqualTo("test.txt");
        assertThat(loadedMessage.getAttachments().get(0).getMessage().getId()).isEqualTo(message.getId());
    }

    @Test
    void testPersistUserMessageWithMultipleEntityReferencesAndAttachments() {
        // given
        AiConversation conversation = createConversation("Multi Context Persistence Test");
        Category firstCategory = createCategory("Multi Ref Category A");
        Category secondCategory = createCategory("Multi Ref Category B");
        Category thirdCategory = createCategory("Multi Ref Category C");

        ChatMessage message = createUserMessage(conversation, "Message with multiple context items");

        ChatMessageEntityReference firstRef = createEntityReference(message, firstCategory, 2);
        ChatMessageEntityReference secondRef = createEntityReference(message, secondCategory, 0);
        ChatMessageEntityReference thirdRef = createEntityReference(message, thirdCategory, 1);
        AiConversationAttachment firstAttachment = createAttachment(message, "first-context.txt");
        AiConversationAttachment secondAttachment = createAttachment(message, "second-context.txt");

        message.setEntityReferences(List.of(firstRef, secondRef, thirdRef));
        message.setAttachments(List.of(firstAttachment, secondAttachment));

        // when
        dataManager.save(new SaveContext().saving(
                message,
                firstRef,
                secondRef,
                thirdRef,
                firstAttachment,
                secondAttachment
        ));

        // then
        ChatMessage loadedMessage = loadMessageWithContext(message.getId());

        assertThat(loadedMessage.getEntityReferences())
                .hasSize(3)
                .extracting(ChatMessageEntityReference::getSortOrder)
                .containsExactly(0, 1, 2);
        assertThat(loadedMessage.getAttachments()).hasSize(2);
        assertThat(loadedMessage.getAttachments())
                .allSatisfy(attachment -> assertThat(attachment.getMessage().getId()).isEqualTo(message.getId()));
    }

    @Test
    void testDeleteChatMessageCascadesEntityReferencesAndAttachments() {
        // given
        AiConversation conversation = createConversation("Cascade Delete Persistence Test");
        Category firstCategory = createCategory("Cascade Ref Category A");
        Category secondCategory = createCategory("Cascade Ref Category B");

        ChatMessage message = createUserMessage(conversation, "Message with context to delete");
        ChatMessageEntityReference firstRef = createEntityReference(message, firstCategory, 0);
        ChatMessageEntityReference secondRef = createEntityReference(message, secondCategory, 1);
        AiConversationAttachment attachment = createAttachment(message, "cascade-context.txt");

        message.setEntityReferences(List.of(firstRef, secondRef));
        message.setAttachments(List.of(attachment));

        dataManager.save(new SaveContext().saving(message, firstRef, secondRef, attachment));

        // when
        dataManager.remove(message);

        // then
        assertThat(countEntityReferences(message.getId())).isZero();
        assertThat(countAttachments(message.getId())).isZero();
    }

    private AiConversation createConversation(String title) {
        return createAndSaveEntity(AiConversation.class, conversation -> {
            conversation.setTitle(title);
            conversation.setCreatedDate(OffsetDateTime.now());
        });
    }

    private Category createCategory(String name) {
        return createAndSaveEntity(Category.class, category -> {
            category.setName(name);
            category.setCode("test-cat-" + UUID.randomUUID());
        });
    }

    private ChatMessage createUserMessage(AiConversation conversation, String content) {
        ChatMessage message = dataManager.create(ChatMessage.class);
        message.setConversation(conversation);
        message.setType(ChatMessageType.USER);
        message.setContent(content);
        message.setCreatedDate(OffsetDateTime.now());
        return message;
    }

    private ChatMessageEntityReference createEntityReference(ChatMessage message, Category category, int sortOrder) {
        ChatMessageEntityReference reference = dataManager.create(ChatMessageEntityReference.class);
        reference.setMessage(message);
        reference.setEntityReference(idSerialization.idToString(Id.of(category)));
        reference.setSortOrder(sortOrder);
        return reference;
    }

    private AiConversationAttachment createAttachment(ChatMessage message, String fileName) {
        AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
        attachment.setMessage(message);
        attachment.setFileName(fileName);
        attachment.setTitle(fileName);
        attachment.setFile(new FileRef("storage", "path/" + fileName, fileName));
        attachment.setOrigin(AiAttachmentOrigin.USER_UPLOADED);
        return attachment;
    }

    private ChatMessage loadMessageWithContext(UUID messageId) {
        return dataManager.load(ChatMessage.class)
                .id(messageId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                        .add("entityReferences", FetchPlan.BASE)
                        .add("attachments", FetchPlan.BASE))
                .one();
    }

    private Long countEntityReferences(UUID messageId) {
        return dataManager.loadValue(
                        "select count(r) from ChatMessageEntityReference r where r.message.id = :messageId",
                        Long.class)
                .parameter("messageId", messageId)
                .one();
    }

    private Long countAttachments(UUID messageId) {
        return dataManager.loadValue(
                        "select count(a) from AiConversationAttachment a where a.message.id = :messageId",
                        Long.class)
                .parameter("messageId", messageId)
                .one();
    }
}
