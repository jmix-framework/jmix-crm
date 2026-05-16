package com.company.crm.test.ai.memory;

import com.company.crm.AbstractTest;
import com.company.crm.ai.memory.JmixChatMemoryRepository;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageAiInputMapperIntegrationTest extends AbstractTest {

    @Autowired
    private AiConversationService aiConversationService;
    @Autowired
    private JmixChatMemoryRepository chatMemoryRepository;
    @Autowired
    private FileStorage fileStorage;
    @Autowired
    private IdSerialization idSerialization;

    @Test
    void testUserMessageWithEntityReferencesAndTextAttachmentLoadsAsSingleSpringUserMessage() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            Client client = entities.client("Mapper Context Client " + UUID.randomUUID());
            Category category = entities.category("Mapper Context Category " + UUID.randomUUID(), "mapper-" + UUID.randomUUID());
            String clientReference = idSerialization.idToString(Id.of(client));
            String categoryReference = idSerialization.idToString(Id.of(category));
            String fileName = "mapper-context-%s.csv".formatted(UUID.randomUUID());
            FileRef fileRef = saveTextFile(fileName, """
                    account,open_amount,overdue_days
                    Mapper Attachment Client,12345.67,42
                    """);

            aiConversationService.createUserMessage(
                    conversation,
                    "Analyse the selected CRM context and the attached file.",
                    List.of(clientReference, categoryReference),
                    List.of(new PendingAttachmentInput(fileRef, fileName))
            );

            List<Message> loadedMessages = chatMemoryRepository.findByConversationId(conversation.getId().toString());
            List<UserMessage> userMessages = loadedMessages.stream()
                    .filter(UserMessage.class::isInstance)
                    .map(UserMessage.class::cast)
                    .toList();

            assertThat(userMessages).hasSize(1);
            UserMessage userMessage = userMessages.getFirst();
            assertThat(userMessage.getText())
                    .contains("Analyse the selected CRM context and the attached file.")
                    .contains("Referenced CRM entities:")
                    .contains(clientReference)
                    .contains(categoryReference)
                    .contains(client.getName())
                    .contains(category.getName())
                    .contains("Attached file: " + fileName)
                    .contains("Mapper Attachment Client")
                    .contains("12345.67")
                    .contains("42");
            assertThat(userMessage.getText()).doesNotContain("Use the newly added conversation context");
            assertThat(userMessage.getMedia()).isEmpty();
        });
    }

    @Test
    void testReferencedClientUsesAiContextFetchPlan() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            Client client = entities.client("AI Context FetchPlan Client " + UUID.randomUUID());
            Category category = entities.category("AI Context FetchPlan Category " + UUID.randomUUID(),
                    "ai-context-category-" + UUID.randomUUID());
            CategoryItem product = entities.categoryItem(
                    "AI Context FetchPlan Product " + UUID.randomUUID(),
                    "ai-context-product-" + UUID.randomUUID(),
                    category,
                    BigDecimal.valueOf(149),
                    UomType.PIECES
            );
            Order order = entities.order(
                    client,
                    "AI-CONTEXT-ORDER-" + UUID.randomUUID(),
                    LocalDate.now(),
                    OrderStatus.NEW,
                    BigDecimal.valueOf(298)
            );
            entities.orderItem(order, product, BigDecimal.valueOf(2));

            aiConversationService.createUserMessage(
                    conversation,
                    "Analyse this client.",
                    List.of(idSerialization.idToString(Id.of(client))),
                    List.of()
            );

            UserMessage userMessage = chatMemoryRepository.findByConversationId(conversation.getId().toString()).stream()
                    .filter(UserMessage.class::isInstance)
                    .map(UserMessage.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertThat(userMessage.getText())
                    .contains("Analyse this client.")
                    .contains(client.getName())
                    .contains(order.getNumber())
                    .contains(product.getName())
                    .contains(category.getName());
        });
    }

    @Test
    void testUserMessageWithImageAttachmentLoadsImageMediaOnly() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            String fileName = "mapper-image-%s.png".formatted(UUID.randomUUID());
            byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
            FileRef fileRef = fileStorage.saveStream(fileName, new ByteArrayInputStream(imageBytes));

            aiConversationService.createUserMessage(
                    conversation,
                    "Look at this image attachment.",
                    List.of(),
                    List.of(new PendingAttachmentInput(fileRef, fileName))
            );

            UserMessage userMessage = chatMemoryRepository.findByConversationId(conversation.getId().toString()).stream()
                    .filter(UserMessage.class::isInstance)
                    .map(UserMessage.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertThat(userMessage.getText()).isEqualTo("Look at this image attachment.");
            assertThat(userMessage.getMedia()).hasSize(1);
            assertThat(userMessage.getMedia().getFirst().getName()).isEqualTo(fileName.replace(".", "_"));
            assertThat(userMessage.getMedia().getFirst().getDataAsByteArray()).isEqualTo(imageBytes);
        });
    }

    @ParameterizedTest(name = "unsupported attachment: {0}")
    @ValueSource(strings = {".pdf", ".xlsx"})
    void testUnsupportedAttachmentIsRenderedAsTextNoticeOnly(String extension) {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            String fileName = "mapper-binary-%s%s".formatted(UUID.randomUUID(), extension);
            String mimeType = mimeTypeForUnsupportedAttachment(extension);
            FileRef fileRef = fileStorage.saveStream(fileName,
                    new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));

            aiConversationService.createUserMessage(
                    conversation,
                    "Analyse the attached file.",
                    List.of(),
                    List.of(new PendingAttachmentInput(fileRef, fileName))
            );

            UserMessage userMessage = chatMemoryRepository.findByConversationId(conversation.getId().toString()).stream()
                    .filter(UserMessage.class::isInstance)
                    .map(UserMessage.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertThat(userMessage.getMedia()).isEmpty();
            assertThat(userMessage.getText())
                    .contains("Analyse the attached file.")
                    .contains("Attached file: " + fileName)
                    .contains("MIME type: " + mimeType)
                    .contains("The file content is not text-readable in this chat context.");
        });
    }

    private FileRef saveTextFile(String fileName, String content) {
        return fileStorage.saveStream(fileName,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String mimeTypeForUnsupportedAttachment(String extension) {
        return switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> throw new IllegalArgumentException("Unsupported test extension: " + extension);
        };
    }
}
