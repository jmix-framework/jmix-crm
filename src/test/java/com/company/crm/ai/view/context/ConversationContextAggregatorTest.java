package com.company.crm.ai.view.context;

import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationContextAggregatorTest {

    private final ConversationContextAggregator aggregator = new ConversationContextAggregator();

    @Test
    void nullConversationProducesEmptyAggregation() {
        ConversationContextItems items = aggregator.aggregate(null);

        assertThat(items.isEmpty()).isTrue();
        assertThat(items.totalCount()).isZero();
        assertThat(items.entityReferences()).isEmpty();
        assertThat(items.generatedAttachments()).isEmpty();
        assertThat(items.uploadedAttachments()).isEmpty();
    }

    @Test
    void conversationWithoutMessagesProducesEmptyAggregation() {
        AiConversation conversation = new AiConversation();

        ConversationContextItems items = aggregator.aggregate(conversation);

        assertThat(items.isEmpty()).isTrue();
    }

    @Test
    void aggregatesEntityReferencesAndSeparatesAttachmentsByType() {
        OffsetDateTime now = OffsetDateTime.now();

        ChatMessage message = newMessage(now);
        message.setEntityReferences(List.of(
                newEntityReference("crm$Client/1"),
                newEntityReference("crm$Client/2")
        ));
        message.setAttachments(List.of(
                newAttachment("ai.csv", AiAttachmentOrigin.AI_GENERATED),
                newAttachment("user.pdf", AiAttachmentOrigin.USER_UPLOADED)
        ));

        AiConversation conversation = new AiConversation();
        conversation.setMessages(List.of(message));

        ConversationContextItems items = aggregator.aggregate(conversation);

        assertThat(items.entityReferences()).containsExactly("crm$Client/1", "crm$Client/2");
        assertThat(items.generatedAttachments()).extracting(AiConversationAttachment::getFileName)
                .containsExactly("ai.csv");
        assertThat(items.uploadedAttachments()).extracting(AiConversationAttachment::getFileName)
                .containsExactly("user.pdf");
        assertThat(items.totalCount()).isEqualTo(4);
    }

    @Test
    void duplicateEntityReferencesAreDeduplicatedWhilePreservingOrder() {
        OffsetDateTime now = OffsetDateTime.now();

        ChatMessage first = newMessage(now);
        first.setEntityReferences(List.of(newEntityReference("crm$Client/1"), newEntityReference("crm$Client/2")));

        ChatMessage second = newMessage(now.plusMinutes(1));
        second.setEntityReferences(List.of(newEntityReference("crm$Client/2"), newEntityReference("crm$Client/3")));

        AiConversation conversation = new AiConversation();
        conversation.setMessages(List.of(first, second));

        ConversationContextItems items = aggregator.aggregate(conversation);

        assertThat(items.entityReferences()).containsExactly(
                "crm$Client/1", "crm$Client/2", "crm$Client/3"
        );
    }

    @Test
    void duplicateEntityReferencesKeepFirstChronologicalOccurrenceWhenMessagesAreUnsorted() {
        OffsetDateTime now = OffsetDateTime.now();

        ChatMessage newerMessage = newMessage(now.plusMinutes(10));
        newerMessage.setEntityReferences(List.of(
                newEntityReference("crm$Client/new-only"),
                newEntityReference("crm$Client/shared")
        ));

        ChatMessage olderMessage = newMessage(now);
        olderMessage.setEntityReferences(List.of(
                newEntityReference("crm$Client/shared"),
                newEntityReference("crm$Client/old-only")
        ));

        AiConversation conversation = new AiConversation();
        conversation.setMessages(new ArrayList<>(List.of(newerMessage, olderMessage)));

        ConversationContextItems items = aggregator.aggregate(conversation);

        assertThat(items.entityReferences()).containsExactly(
                "crm$Client/shared",
                "crm$Client/old-only",
                "crm$Client/new-only"
        );
    }

    @Test
    void blankAndNullEntityReferencesAreFiltered() {
        ChatMessage message = newMessage(OffsetDateTime.now());
        message.setEntityReferences(List.of(
                newEntityReference("crm$Client/1"),
                newEntityReference(""),
                newEntityReference("   "),
                newEntityReference(null)
        ));

        AiConversation conversation = new AiConversation();
        conversation.setMessages(List.of(message));

        ConversationContextItems items = aggregator.aggregate(conversation);

        assertThat(items.entityReferences()).containsExactly("crm$Client/1");
    }

    @Test
    void messagesAreSortedByCreatedDateBeforeAggregation() {
        OffsetDateTime later = OffsetDateTime.now();
        OffsetDateTime earlier = later.minusHours(1);

        ChatMessage olderMessage = newMessage(earlier);
        olderMessage.setAttachments(List.of(newAttachment("a.csv", AiAttachmentOrigin.AI_GENERATED)));
        ChatMessage newerMessage = newMessage(later);
        newerMessage.setAttachments(List.of(newAttachment("b.csv", AiAttachmentOrigin.AI_GENERATED)));

        AiConversation conversation = new AiConversation();
        // Intentionally pass in reverse chronological order — aggregator must reorder.
        conversation.setMessages(new ArrayList<>(List.of(newerMessage, olderMessage)));

        ConversationContextItems items = aggregator.aggregate(conversation);

        assertThat(items.generatedAttachments()).extracting(AiConversationAttachment::getFileName)
                .containsExactly("a.csv", "b.csv");
    }

    @Test
    void missingAttachmentTypeIsTreatedAsUploaded() {
        ChatMessage message = newMessage(OffsetDateTime.now());
        message.setAttachments(List.of(newAttachment("unknown.txt", null)));

        AiConversation conversation = new AiConversation();
        conversation.setMessages(List.of(message));

        ConversationContextItems items = aggregator.aggregate(conversation);

        assertThat(items.generatedAttachments()).isEmpty();
        assertThat(items.uploadedAttachments()).hasSize(1);
    }

    private ChatMessage newMessage(OffsetDateTime createdDate) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setType(ChatMessageType.ASSISTANT);
        message.setCreatedDate(createdDate);
        return message;
    }

    private ChatMessageEntityReference newEntityReference(String reference) {
        ChatMessageEntityReference ref = new ChatMessageEntityReference();
        ref.setEntityReference(reference);
        return ref;
    }

    private AiConversationAttachment newAttachment(String fileName, AiAttachmentOrigin origin) {
        AiConversationAttachment attachment = new AiConversationAttachment();
        attachment.setFileName(fileName);
        attachment.setOrigin(origin);
        return attachment;
    }
}
