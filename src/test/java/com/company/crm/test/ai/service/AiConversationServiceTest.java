package com.company.crm.test.ai.service;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.client.Client;
import io.jmix.core.FetchPlan;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiConversationServiceTest extends AbstractTest {

    @Autowired
    private AiConversationService aiConversationService;
    @Autowired
    private IdSerialization idSerialization;

    @Test
    void testCreateUserMessageRequiresHumanText() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            Client client = entities.client("Composer Validation Client");
            String clientReference = idSerialization.idToString(Id.of(client));

            assertThatThrownBy(() -> aiConversationService.createUserMessage(
                    conversation,
                    " ",
                    List.of(clientReference),
                    List.of(new PendingAttachmentInput(fileRef("validation.txt"), "validation.txt"))
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("human message text");

            long userMessages = dataManager.loadValue(
                            "select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.type = :type",
                            Long.class)
                    .parameter("conversationId", conversation.getId())
                    .parameter("type", ChatMessageType.USER.getId())
                    .one();
            assertThat(userMessages).isZero();
        });
    }

    @Test
    void testCreateUserMessageAndEnsureStartedDoesNotMarkInvalidPromptAsStarted() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();

            assertThatThrownBy(() -> aiConversationService.createUserMessageAndEnsureStarted(
                    conversation,
                    " ",
                    List.of(),
                    List.of()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("human message text");

            AiConversation reloaded = dataManager.load(AiConversation.class)
                    .id(conversation.getId())
                    .one();

            assertThat(reloaded.getFirstMessageSent()).isFalse();
        });
    }

    @Test
    void testCreateUserMessageAndEnsureStartedMarksConversationAsStarted() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();

            ChatMessage message = aiConversationService.createUserMessageAndEnsureStarted(
                    conversation,
                    " Start this conversation ",
                    List.of(),
                    List.of()
            );

            AiConversation reloaded = dataManager.load(AiConversation.class)
                    .id(conversation.getId())
                    .one();

            assertThat(message.getContent()).isEqualTo("Start this conversation");
            assertThat(reloaded.getFirstMessageSent()).isTrue();
        });
    }

    @Test
    void testCreateUserMessagePersistsTextReferencesAndAttachmentsTogether() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            Client client = entities.client("Composer Client");
            Category category = entities.category("Composer Category", "composer-" + UUID.randomUUID());
            String clientReference = idSerialization.idToString(Id.of(client));
            String categoryReference = idSerialization.idToString(Id.of(category));

            ChatMessage message = aiConversationService.createUserMessage(
                    conversation,
                    "Please analyse these context items together.",
                    List.of(clientReference, categoryReference, clientReference),
                    List.of(
                            new PendingAttachmentInput(fileRef("composer-a.csv"), "composer-a.csv"),
                            new PendingAttachmentInput(fileRef("composer-b.txt"), "composer-b.txt")
                    )
            );

            ChatMessage reloaded = dataManager.load(ChatMessage.class)
                    .id(message.getId())
                    .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                            .add("entityReferences", FetchPlan.BASE)
                            .add("attachments", FetchPlan.BASE))
                    .one();

            assertThat(reloaded.getType()).isEqualTo(ChatMessageType.USER);
            assertThat(reloaded.getContent()).isEqualTo("Please analyse these context items together.");
            assertThat(reloaded.getEntityReferences())
                    .extracting(ChatMessageEntityReference::getEntityReference)
                    .containsExactly(clientReference, categoryReference);
            assertThat(reloaded.getEntityReferences())
                    .extracting(ChatMessageEntityReference::getSortOrder)
                    .containsExactly(0, 1);
            assertThat(reloaded.getAttachments())
                    .extracting(AiConversationAttachment::getFileName)
                    .containsExactly("composer-a.csv", "composer-b.txt");
            assertThat(reloaded.getAttachments())
                    .allSatisfy(attachment -> assertThat(attachment.getMessage().getId()).isEqualTo(message.getId()));
        });
    }

    @Test
    void testDuplicateEntityReferenceIsIgnoredByLinkedHashSetSemantics() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            Client client = entities.client("Dedupe Client");
            Category category = entities.category("Dedupe Category", "dedupe-" + UUID.randomUUID());
            String refA = idSerialization.idToString(Id.of(client));
            String refB = idSerialization.idToString(Id.of(category));

            ChatMessage message = aiConversationService.createUserMessage(
                    conversation,
                    "Dedupe",
                    List.of(refA, refB, refA, refB, refA),
                    List.of()
            );

            ChatMessage reloaded = dataManager.load(ChatMessage.class)
                    .id(message.getId())
                    .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                            .add("entityReferences", FetchPlan.BASE))
                    .one();

            assertThat(reloaded.getEntityReferences())
                    .extracting(ChatMessageEntityReference::getEntityReference)
                    .containsExactly(refA, refB);
            assertThat(reloaded.getEntityReferences())
                    .extracting(ChatMessageEntityReference::getSortOrder)
                    .containsExactly(0, 1);
        });
    }

    @Test
    void testDuplicateOnlyEntityReferenceProducesSingleChild() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            Client client = entities.client("Single Dedupe Client");
            String refA = idSerialization.idToString(Id.of(client));

            ChatMessage message = aiConversationService.createUserMessage(
                    conversation,
                    "Single",
                    List.of(refA, refA, refA),
                    List.of()
            );

            ChatMessage reloaded = dataManager.load(ChatMessage.class)
                    .id(message.getId())
                    .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                            .add("entityReferences", FetchPlan.BASE))
                    .one();

            assertThat(reloaded.getEntityReferences()).hasSize(1);
            assertThat(reloaded.getEntityReferences().getFirst().getEntityReference()).isEqualTo(refA);
            assertThat(reloaded.getEntityReferences().getFirst().getSortOrder()).isZero();
        });
    }

    private FileRef fileRef(String fileName) {
        return new FileRef("storage", "test/" + fileName, fileName);
    }
}
