package com.company.crm.ai.jmix.memory;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
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

class JmixChatMemoryRepositoryIntegrationTest extends AbstractTest {

    @Autowired
    private JmixChatMemoryRepository chatMemoryRepository;
    @Autowired
    private AiConversationService aiConversationService;

    @Test
    void testSaveAndLoadMessages() {
        AiConversation conversation = aiConversationService.createNewConversation("Test conversation");
        String conversationId = conversation.getId().toString();

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

    }

    @Test
    void testReplaceSemantics() {
        AiConversation conversation = aiConversationService.createNewConversation("Replace test conversation");
        String conversationId = conversation.getId().toString();

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

    }

    @Test
    void testMessageTypeConversions() {
        AiConversation conversation = aiConversationService.createNewConversation("Message type test");
        String conversationId = conversation.getId().toString();

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

    }

    @Test
    void testConversationCreation() {
        AiConversation conversation = aiConversationService.createNewConversation("Conversation creation test");
        String conversationId = conversation.getId().toString();

        // Save messages to pre-created conversation
        List<Message> messages = List.of(new UserMessage("Test message"));
        chatMemoryRepository.saveAll(conversationId, messages);

        // Verify conversation exists in conversation IDs
        List<String> idsAfterSave = chatMemoryRepository.findConversationIds();
        assertThat(idsAfterSave).contains(conversationId);

        // Verify conversation exists in database
        UUID uuid = UUID.fromString(conversationId);
        AiConversation reloadedConversation = dataManager.load(AiConversation.class).id(uuid).one();
        assertThat(reloadedConversation).isNotNull();
        assertThat(reloadedConversation.getId()).isEqualTo(uuid);
    }

    @Test
    void testMultipleConversationIsolation() {
        AiConversation conversation1 = aiConversationService.createNewConversation("Conversation 1");
        AiConversation conversation2 = aiConversationService.createNewConversation("Conversation 2");
        String conversation1Id = conversation1.getId().toString();
        String conversation2Id = conversation2.getId().toString();

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

    }

    @Test
    void testDeleteConversation() {
        AiConversation conversation = aiConversationService.createNewConversation("welcome");
        String conversationId = conversation.getId().toString();

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
        AiConversation reloadedConversation = dataManager.load(AiConversation.class).id(uuid).optional().orElse(null);
        assertThat(reloadedConversation).isNull();

    }


    @Test
    void testMessageOrdering() {
        AiConversation conversation = aiConversationService.createNewConversation("Message ordering test");
        String conversationId = conversation.getId().toString();

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

    }

    @Test
    void testEntityMappingConsistency() {
        AiConversation conversation = aiConversationService.createNewConversation("Entity mapping test");
        String conversationId = conversation.getId().toString();

        // Save a message and verify entity mapping
        List<Message> messages = List.of(new UserMessage("Test content"));
        chatMemoryRepository.saveAll(conversationId, messages);

        // Check entity directly in database
        UUID uuid = UUID.fromString(conversationId);
        List<ChatMessage> chatMessages = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId")
            .parameter("conversationId", uuid)
            .list();

        assertThat(chatMessages).hasSize(1);
        ChatMessage firstChatMessage = chatMessages.get(0);
        assertThat(firstChatMessage.getContent()).isEqualTo("Test content");
        assertThat(firstChatMessage.getType()).isEqualTo(ChatMessageType.USER);
        assertThat(firstChatMessage.getConversation().getId()).isEqualTo(uuid);

    }

    @Test
    void testIncrementalSaveDoesNotDuplicateMessages() {
        AiConversation conversation = aiConversationService.createNewConversation("Incremental save test");
        String conversationId = conversation.getId().toString();

        // Save initial messages
        List<Message> initialMessages = List.of(
            new UserMessage("First message"),
            new AssistantMessage("First response")
        );
        chatMemoryRepository.saveAll(conversationId, initialMessages);

        // Load messages to get their entityIds and timestamps
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(loadedMessages).hasSize(2);

        // Extract original timestamps by checking the database directly
        UUID uuid = UUID.fromString(conversationId);
        List<ChatMessage> originalChatMessages = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdDate")
            .parameter("conversationId", uuid)
            .list();

        assertThat(originalChatMessages).hasSize(2);
        var firstOriginalTimestamp = originalChatMessages.get(0).getCreatedDate();
        var secondOriginalTimestamp = originalChatMessages.get(1).getCreatedDate();
        var firstOriginalId = originalChatMessages.get(0).getId();
        var secondOriginalId = originalChatMessages.get(1).getId();

        // Wait a bit to ensure new timestamps would be different
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Save the same messages again (they should have entityIds now from the previous load)
        // This should NOT create duplicates - existing messages should be ignored
        chatMemoryRepository.saveAll(conversationId, loadedMessages);

        // Verify no duplicates were created and timestamps remain unchanged
        List<ChatMessage> reloadedChatMessages = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdDate")
            .parameter("conversationId", uuid)
            .list();

        // Still only 2 messages (no duplicates)
        assertThat(reloadedChatMessages).hasSize(2);

        // Find messages by content to match them correctly
        ChatMessage firstReloaded = reloadedChatMessages.stream()
            .filter(msg -> "First message".equals(msg.getContent()))
            .findFirst()
            .orElseThrow();
        ChatMessage secondReloaded = reloadedChatMessages.stream()
            .filter(msg -> "First response".equals(msg.getContent()))
            .findFirst()
            .orElseThrow();

        // Verify timestamps are exactly the same (nothing was touched)
        assertThat(firstReloaded.getCreatedDate()).isEqualTo(firstOriginalTimestamp);
        assertThat(secondReloaded.getCreatedDate()).isEqualTo(secondOriginalTimestamp);

        // Verify IDs are preserved too (same entities)
        assertThat(firstReloaded.getId()).isEqualTo(firstOriginalId);
        assertThat(secondReloaded.getId()).isEqualTo(secondOriginalId);

        // Verify content is still correct
        assertThat(firstReloaded.getContent()).isEqualTo("First message");
        assertThat(secondReloaded.getContent()).isEqualTo("First response");
        assertThat(firstReloaded.getType()).isEqualTo(ChatMessageType.USER);
        assertThat(secondReloaded.getType()).isEqualTo(ChatMessageType.ASSISTANT);
    }
}