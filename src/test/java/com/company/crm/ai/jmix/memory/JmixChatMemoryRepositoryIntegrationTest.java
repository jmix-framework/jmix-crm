package com.company.crm.ai.jmix.memory;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.security.role.AiChatUserRole;
import com.company.crm.security.role.ManagerRole;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JmixChatMemoryRepositoryIntegrationTest extends AbstractTest {

    @Autowired
    private JmixChatMemoryRepository chatMemoryRepository;
    @Autowired
    private AiConversationService aiConversationService;

        @Test
        void testSaveAndLoadMessages() {
            // given
            AiConversation conversation = aiConversationService.createNewConversation("Welcome");
            String conversationId = conversation.getId().toString();
    
            // Create simple test messages
            List<Message> originalMessages = List.of(
                new UserMessage("Message 1"),
                new AssistantMessage("Message 2")
            );
    
            // when
            chatMemoryRepository.saveAll(conversationId, originalMessages);
    
            // then
            List<String> conversationIds = chatMemoryRepository.findConversationIds();
            assertThat(conversationIds).contains(conversationId);
    
            // Load messages back and verify count (1 welcome + 2 new)
            List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
            assertThat(loadedMessages).hasSize(3);
    
            // Simple content check
            assertThat(loadedMessages.stream().map(Message::getText).toList())
                .containsExactly("Welcome", "Message 1", "Message 2");
    
        }
    
        @Test
        void testAdditiveSemantics() {
            // given
            AiConversation conversation = aiConversationService.createNewConversation("Welcome");
            String conversationId = conversation.getId().toString();
    
            // when
            List<Message> initialMessages = List.of(
                new UserMessage("First message"),
                new AssistantMessage("First response")
            );
            chatMemoryRepository.saveAll(conversationId, initialMessages);
    
            // then
            List<Message> firstLoad = chatMemoryRepository.findByConversationId(conversationId);
            assertThat(firstLoad).hasSize(3);
    
            // Save different messages to same conversation (should be additive now, no more replacement)
            List<Message> additionalMessages = List.of(
                new UserMessage("New first message"),
                new AssistantMessage("New first response"),
                new UserMessage("Additional message")
            );
            chatMemoryRepository.saveAll(conversationId, additionalMessages);
    
            // Verify additive behavior (1 welcome + 2 initial + 3 additional)
            List<Message> secondLoad = chatMemoryRepository.findByConversationId(conversationId);
            assertThat(secondLoad).hasSize(6);
            assertThat(secondLoad.stream().map(Message::getText).toList())
                .containsExactly("Welcome", "First message", "First response", "New first message", "New first response", "Additional message");
        }

    @Test
    void testMessageTypeConversions() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Welcome");
        String conversationId = conversation.getId().toString();

        // Create messages of different types
        List<Message> mixedMessages = List.of(
            new UserMessage("User question"),
            new AssistantMessage("Assistant response"),
            new SystemMessage("System message for context")
        );

        // when
        chatMemoryRepository.saveAll(conversationId, mixedMessages);
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);

        // then
        assertThat(loadedMessages).hasSize(4);
        
        // Find by content to be stable
        Message userMsg = loadedMessages.stream().filter(m -> "User question".equals(m.getText())).findFirst().orElseThrow();
        Message assistantMsg = loadedMessages.stream().filter(m -> "Assistant response".equals(m.getText())).findFirst().orElseThrow();
        Message systemMsg = loadedMessages.stream().filter(m -> "System message for context".equals(m.getText())).findFirst().orElseThrow();

        assertThat(userMsg).isInstanceOf(UserMessage.class);
        assertThat(assistantMsg).isInstanceOf(AssistantMessage.class);
        assertThat(systemMsg).isInstanceOf(SystemMessage.class);
    }

    @Test
    void testAttachmentMessageMetadataMapsToAttachmentType() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Welcome");
        String conversationId = conversation.getId().toString();

        UserMessage uploadMessage = UserMessage.builder()
                .text("Attachment \"report.csv\" added")
                .metadata(Map.of("crmMessageType", "ATTACHMENT"))
                .build();

        // when
        chatMemoryRepository.saveAll(conversationId, List.of(uploadMessage));

        UUID uuid = UUID.fromString(conversationId);
        ChatMessage uploadEntity = dataManager.load(ChatMessage.class)
                .query("select m from ChatMessage m where m.conversation.id = :conversationId and m.content = :content")
                .parameter("conversationId", uuid)
                .parameter("content", "Attachment \"report.csv\" added")
                .one();

        // then
        assertThat(uploadEntity.getType()).isEqualTo(ChatMessageType.ATTACHMENT);

        Message loadedUploadMessage = chatMemoryRepository.findByConversationId(conversationId).stream()
                .filter(message -> "Attachment \"report.csv\" added".equals(message.getText()))
                .findFirst()
                .orElseThrow();

        assertThat(loadedUploadMessage).isInstanceOf(UserMessage.class);
        assertThat(loadedUploadMessage.getMetadata())
                .containsEntry("crmMessageType", "ATTACHMENT");
    }

    @Test
    void testConversationCreation() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Welcome");
        String conversationId = conversation.getId().toString();

        // when
        List<Message> messages = List.of(new UserMessage("Test message"));
        chatMemoryRepository.saveAll(conversationId, messages);

        // then
        List<String> idsAfterSave = chatMemoryRepository.findConversationIds();
        assertThat(idsAfterSave).contains(conversationId);

        // Verify messages exist (1 welcome + 1 new)
        assertThat(chatMemoryRepository.findByConversationId(conversationId)).hasSize(2);

        // Verify conversation exists in database
        UUID uuid = UUID.fromString(conversationId);
        AiConversation reloadedConversation = dataManager.load(AiConversation.class).id(uuid).one();
        assertThat(reloadedConversation).isNotNull();
        assertThat(reloadedConversation.getId()).isEqualTo(uuid);
    }

    @Test
    void testMultipleConversationIsolation() {
        // given
        AiConversation conversation1 = aiConversationService.createNewConversation("Welcome 1");
        AiConversation conversation2 = aiConversationService.createNewConversation("Welcome 2");
        String conversation1Id = conversation1.getId().toString();
        String conversation2Id = conversation2.getId().toString();

        // when
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

        // then
        List<Message> conv1Loaded = chatMemoryRepository.findByConversationId(conversation1Id);
        List<Message> conv2Loaded = chatMemoryRepository.findByConversationId(conversation2Id);

        // 1 welcome + 1 new
        assertThat(conv1Loaded).hasSize(2);
        assertThat(conv1Loaded.stream().map(Message::getText).toList()).containsExactly("Welcome 1", "Conversation 1 message");

        // 1 welcome + 2 new
        assertThat(conv2Loaded).hasSize(3);
        assertThat(conv2Loaded.stream().map(Message::getText).toList()).containsExactly("Welcome 2", "Conversation 2 message", "Conversation 2 response");

    }

    @Test
    void testDeleteConversation() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("welcome");
        String conversationId = conversation.getId().toString();

        // when
        List<Message> messages = List.of(
            new UserMessage("Message to be deleted"),
            new AssistantMessage("Response to be deleted")
        );
        chatMemoryRepository.saveAll(conversationId, messages);

        // then
        List<Message> beforeDelete = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(beforeDelete).hasSize(3);

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
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Welcome");
        String conversationId = conversation.getId().toString();

        // when
        List<Message> orderedMessages = List.of(
            new UserMessage("First message"),
            new AssistantMessage("Second message"),
            new UserMessage("Third message"),
            new AssistantMessage("Fourth message"),
            new UserMessage("Fifth message")
        );

        chatMemoryRepository.saveAll(conversationId, orderedMessages);

        // then
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(loadedMessages).hasSize(6);

        assertThat(loadedMessages.get(0).getText()).isEqualTo("Welcome");
        for (int i = 0; i < orderedMessages.size(); i++) {
            assertThat(loadedMessages.get(i + 1).getText())
                .isEqualTo(orderedMessages.get(i).getText());
        }

    }

    @Test
    void testEntityMappingConsistency() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Welcome");
        String conversationId = conversation.getId().toString();

        // when
        List<Message> messages = List.of(new UserMessage("Test content"));
        chatMemoryRepository.saveAll(conversationId, messages);

        // then
        UUID uuid = UUID.fromString(conversationId);
        List<ChatMessage> chatMessages = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdDate, m.id")
            .parameter("conversationId", uuid)
            .list();

        // 1 welcome + 1 new
        assertThat(chatMessages).hasSize(2);
        
        // Use content-based matching instead of index assuming
        ChatMessage testContentEntity = chatMessages.stream()
            .filter(m -> "Test content".equals(m.getContent()))
            .findFirst()
            .orElseThrow();
            
        assertThat(testContentEntity.getContent()).isEqualTo("Test content");
        assertThat(testContentEntity.getType()).isEqualTo(ChatMessageType.USER);
        assertThat(testContentEntity.getConversation().getId()).isEqualTo(uuid);

    }

    @Test
    void testIsolationBetweenUsers() {
        // given
        final String[] conversationIdWrapper = new String[1];
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation("Admin Private Chat");
            conversationIdWrapper[0] = conversation.getId().toString();
        });
        String conversationId = conversationIdWrapper[0];

        // Manager user gets ai-chat row-level role, but conversation remains system-owned/private.
        systemAuthenticator.runWithSystem(() -> {
            testUsers.assignRowLevelRole(ManagerRole.CODE, AiChatUserRole.CODE);
            List<Message> systemVisibleMessages = chatMemoryRepository.findByConversationId(conversationId);
            assertThat(systemVisibleMessages.stream().map(Message::getText).toList())
                    .containsExactly("Admin Private Chat");
        });

        // when / then
        runWithManager(() -> {
            // Memory Repository should NOT find it
            List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
            assertThat(loadedMessages).isEmpty();

            // Conversation list should NOT contain it
            List<String> conversationIds = chatMemoryRepository.findConversationIds();
            assertThat(conversationIds).doesNotContain(conversationId);
        });

        // then
        systemAuthenticator.runWithSystem(() -> {
            List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
            assertThat(loadedMessages.stream().map(Message::getText).toList())
                    .containsExactly("Admin Private Chat");
        });
    }

    @Test
    void testSaveSubsetDoesNotDeleteOldMessages() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Welcome");
        String conversationId = conversation.getId().toString();

        // when
        List<Message> initialMessages = List.of(
            new UserMessage("Msg 1"),
            new AssistantMessage("Resp 1"),
            new UserMessage("Msg 2"),
            new AssistantMessage("Resp 2")
        );
        chatMemoryRepository.saveAll(conversationId, initialMessages);

        // then
        assertThat(chatMemoryRepository.findByConversationId(conversationId)).hasSize(5);

        // when
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
        List<Message> lastTwoMessages = loadedMessages.subList(3, 5); // [Msg 2, Resp 2]
        
        chatMemoryRepository.saveAll(conversationId, lastTwoMessages);

        // then
        UUID uuid = UUID.fromString(conversationId);
        List<ChatMessage> allEntities = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdDate, m.id")
            .parameter("conversationId", uuid)
            .list();

        assertThat(allEntities).hasSize(5);
        assertThat(allEntities.stream().map(ChatMessage::getContent).toList())
            .containsExactly("Welcome", "Msg 1", "Resp 1", "Msg 2", "Resp 2");
    }

    @Test
    void testIncrementalSaveDoesNotDuplicateMessages() {
        // given
        AiConversation conversation = aiConversationService.createNewConversation("Incremental save test");
        String conversationId = conversation.getId().toString();

        // when
        List<Message> initialMessages = List.of(
            new UserMessage("First message"),
            new AssistantMessage("First response")
        );
        chatMemoryRepository.saveAll(conversationId, initialMessages);

        // then
        List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversationId);
        assertThat(loadedMessages).hasSize(3); // 1 welcome + 2 initial

        // Extract original timestamps by checking the database directly
        UUID uuid = UUID.fromString(conversationId);
        List<ChatMessage> originalChatMessages = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdDate, m.id")
            .parameter("conversationId", uuid)
            .list();

        assertThat(originalChatMessages).hasSize(3);
        
        ChatMessage firstOriginalEntity = originalChatMessages.stream()
            .filter(msg -> "First message".equals(msg.getContent()))
            .findFirst()
            .orElseThrow();
        ChatMessage secondOriginalEntity = originalChatMessages.stream()
            .filter(msg -> "First response".equals(msg.getContent()))
            .findFirst()
            .orElseThrow();

        var firstOriginalTimestamp = firstOriginalEntity.getCreatedDate();
        var secondOriginalTimestamp = secondOriginalEntity.getCreatedDate();
        var firstOriginalId = firstOriginalEntity.getId();
        var secondOriginalId = secondOriginalEntity.getId();

        // when
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Save the same messages again (they should have entityIds now from the previous load)
        // This should NOT create duplicates - existing messages should be ignored
        chatMemoryRepository.saveAll(conversationId, loadedMessages);

        // then
        List<ChatMessage> reloadedChatMessages = dataManager.load(ChatMessage.class)
            .query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdDate, m.id")
            .parameter("conversationId", uuid)
            .list();

        // Still only 3 messages (no duplicates)
        assertThat(reloadedChatMessages).hasSize(3);

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
