package com.company.crm.ai.jmix.memory;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for JmixChatMemoryRepository, testing persistent chat memory functionality
 * with Spring AI ChatMemoryRepository interface and Jmix entities.
 */
class JmixChatMemoryRepositoryIntegrationTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(JmixChatMemoryRepositoryIntegrationTest.class);

    @Autowired
    private JmixChatMemoryRepository chatMemoryRepository;

    @Test
    void testSaveAndLoadMessages() {
        String conversationId = UUID.randomUUID().toString();

        // Create simple test messages
        List<Message> originalMessages = List.of(
            new UserMessage("Message 1"),
            new AssistantMessage("Message 2")
        );

        // Save messages
        chatMemoryRepository.saveAll(conversationId, originalMessages);

        // Verify conversation was created by checking conversation IDs
        List<String> conversationIds = chatMemoryRepository.findConversationIds();
        assertThat(conversationIds).contains(conversationId);

        // Load messages back and verify count
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(loadedMessages).hasSize(2);

        // Simple content check without strict ordering for now
        assertThat(loadedMessages.stream().map(Message::getText).toList())
            .containsExactlyInAnyOrder("Message 1", "Message 2");

        log.info("Successfully tested basic save and load");
    }

    @Test
    void testReplaceSemantics() {
        String conversationId = UUID.randomUUID().toString();

        // Save initial messages
        List<Message> initialMessages = List.of(
            new UserMessage("First message"),
            new AssistantMessage("First response")
        );
        chatMemoryRepository.saveAll(conversationId, initialMessages);

        // Verify initial save
        List<Message> firstLoad = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(firstLoad).hasSize(2);

        // Save different messages to same conversation (should replace)
        List<Message> replacementMessages = List.of(
            new UserMessage("New first message"),
            new AssistantMessage("New first response"),
            new UserMessage("Additional message")
        );
        chatMemoryRepository.saveAll(conversationId, replacementMessages);

        // Verify replacement
        List<Message> secondLoad = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(secondLoad).hasSize(3);
        assertThat(secondLoad.get(0).getText()).isEqualTo("New first message");
        assertThat(secondLoad.get(1).getText()).isEqualTo("New first response");
        assertThat(secondLoad.get(2).getText()).isEqualTo("Additional message");

        log.info("Successfully tested replace semantics with {} replacement messages", secondLoad.size());
    }

    @Test
    void testMessageTypeConversions() {
        String conversationId = UUID.randomUUID().toString();

        // Create messages of different types
        List<Message> mixedMessages = List.of(
            new UserMessage("User question"),
            new AssistantMessage("Assistant response"),
            new SystemMessage("System message for context")
        );

        chatMemoryRepository.saveAll(conversationId, mixedMessages);
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);

        // Verify type conversion
        assertThat(loadedMessages).hasSize(3);
        assertThat(loadedMessages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(loadedMessages.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(loadedMessages.get(2)).isInstanceOf(SystemMessage.class);

        // Verify content preservation
        assertThat(loadedMessages.get(0).getText()).isEqualTo("User question");
        assertThat(loadedMessages.get(1).getText()).isEqualTo("Assistant response");
        assertThat(loadedMessages.get(2).getText()).isEqualTo("System message for context");

        log.info("Successfully tested message type conversions for all Spring AI message types");
    }

    @Test
    void testConversationCreation() {
        String conversationId = UUID.randomUUID().toString();

        // Initially no conversations
        List<String> emptyIds = chatMemoryRepository.findConversationIds();
        int initialCount = emptyIds.size();

        // Save messages should create conversation
        List<Message> messages = List.of(new UserMessage("Test message"));
        chatMemoryRepository.saveAll(conversationId, messages);

        // Verify conversation was created
        List<String> idsAfterSave = chatMemoryRepository.findConversationIds();
        assertThat(idsAfterSave).hasSize(initialCount + 1);
        assertThat(idsAfterSave).contains(conversationId);

        // Verify conversation exists in database
        UUID uuid = UUID.fromString(conversationId);
        AiConversation conversation = dataManager.load(AiConversation.class).id(uuid).one();
        assertThat(conversation).isNotNull();
        assertThat(conversation.getId()).isEqualTo(uuid);
        assertThat(conversation.getTitle()).contains("Chat ");

        log.info("Successfully tested conversation creation with title: {}", conversation.getTitle());
    }

    @Test
    void testMultipleConversationIsolation() {
        String conversation1Id = UUID.randomUUID().toString();
        String conversation2Id = UUID.randomUUID().toString();

        // Save messages to conversation 1
        List<Message> conv1Messages = List.of(
            new UserMessage("Conversation 1 message")
        );
        chatMemoryRepository.saveAll(conversation1Id, conv1Messages);

        // Save messages to conversation 2
        List<Message> conv2Messages = List.of(
            new UserMessage("Conversation 2 message"),
            new AssistantMessage("Conversation 2 response")
        );
        chatMemoryRepository.saveAll(conversation2Id, conv2Messages);

        // Verify isolation
        List<Message> conv1Loaded = chatMemoryRepository.findByConversationId(conversation1Id);
        List<Message> conv2Loaded = chatMemoryRepository.findByConversationId(conversation2Id);

        assertThat(conv1Loaded).hasSize(1);
        assertThat(conv1Loaded.get(0).getText()).isEqualTo("Conversation 1 message");

        assertThat(conv2Loaded).hasSize(2);
        assertThat(conv2Loaded.get(0).getText()).isEqualTo("Conversation 2 message");
        assertThat(conv2Loaded.get(1).getText()).isEqualTo("Conversation 2 response");

        log.info("Successfully tested isolation between {} conversations", 2);
    }

    @Test
    void testDeleteConversation() {
        String conversationId = UUID.randomUUID().toString();

        // Create conversation with messages
        List<Message> messages = List.of(
            new UserMessage("Message to be deleted"),
            new AssistantMessage("Response to be deleted")
        );
        chatMemoryRepository.saveAll(conversationId, messages);

        // Verify messages exist
        List<Message> beforeDelete = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(beforeDelete).hasSize(2);

        // Delete conversation
        chatMemoryRepository.deleteByConversationId(conversationId);

        // Verify conversation and messages are gone
        List<Message> afterDelete = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(afterDelete).isEmpty();

        // Verify conversation doesn't exist in database
        UUID uuid = UUID.fromString(conversationId);
        AiConversation conversation = dataManager.load(AiConversation.class).id(uuid).optional().orElse(null);
        assertThat(conversation).isNull();

        log.info("Successfully tested conversation deletion");
    }

    @Test
    void testEmptyAndNullConversations() {
        // Test non-existent conversation
        List<Message> nonExistent = chatMemoryRepository.findByConversationId(UUID.randomUUID().toString());
        assertThat(nonExistent).isEmpty();

        // Test null conversation ID
        List<Message> nullConv = chatMemoryRepository.findByConversationId(null);
        assertThat(nullConv).isEmpty();

        // Test empty conversation ID
        List<Message> emptyConv = chatMemoryRepository.findByConversationId("");
        assertThat(emptyConv).isEmpty();

        // Test invalid UUID format
        List<Message> invalidUuid = chatMemoryRepository.findByConversationId("invalid-uuid");
        assertThat(invalidUuid).isEmpty();

        log.info("Successfully tested error handling for invalid conversation IDs");
    }

    @Test
    void testMessageOrdering() {
        String conversationId = UUID.randomUUID().toString();

        // Save messages and verify they maintain order
        List<Message> orderedMessages = List.of(
            new UserMessage("First message"),
            new AssistantMessage("Second message"),
            new UserMessage("Third message"),
            new AssistantMessage("Fourth message"),
            new UserMessage("Fifth message")
        );

        chatMemoryRepository.saveAll(conversationId, orderedMessages);

        // Load and verify order
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(loadedMessages).hasSize(5);

        for (int i = 0; i < orderedMessages.size(); i++) {
            assertThat(loadedMessages.get(i).getText())
                .isEqualTo(orderedMessages.get(i).getText());
        }

        log.info("Successfully tested message ordering preservation");
    }

    @Test
    void testEntityMappingConsistency() {
        String conversationId = UUID.randomUUID().toString();

        // Save a message and verify entity mapping
        List<Message> messages = List.of(new UserMessage("Test content"));
        chatMemoryRepository.saveAll(conversationId, messages);

        // Check entity directly in database
        UUID uuid = UUID.fromString(conversationId);
        List<ChatMessage> entities = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId")
            .parameter("conversationId", uuid)
            .list();

        assertThat(entities).hasSize(1);
        ChatMessage entity = entities.get(0);
        assertThat(entity.getContent()).isEqualTo("Test content");
        assertThat(entity.getType()).isEqualTo(ChatMessageType.USER);
        assertThat(entity.getConversation().getId()).isEqualTo(uuid);

        log.info("Successfully verified entity mapping consistency");
    }
}