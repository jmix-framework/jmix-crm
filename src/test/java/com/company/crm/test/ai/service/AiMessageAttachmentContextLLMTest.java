package com.company.crm.test.ai.service;

import com.company.crm.AbstractAiTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.CrmAnalyticsService;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.util.ai.LLMJudge;
import com.company.crm.util.ai.LLMJudgeBuilder;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiMessageAttachmentContextLLMTest extends AbstractAiTest {

    @Autowired
    private AiConversationService aiConversationService;
    @Autowired
    private CrmAnalyticsService crmAnalyticsService;
    @Autowired
    private FileStorage fileStorage;
    @Autowired
    private IdSerialization idSerialization;
    @Autowired
    private LLMJudgeBuilder llmJudgeBuilder;

    private LLMJudge llmJudge;

    @BeforeEach
    @Override
    protected void beforeEach() {
        this.llmJudge = llmJudgeBuilder
                .systemPrompt("""
                        You are an LLM Judge. Evaluate whether an AI response uses attached file context correctly.
                        Always call submitJudgement(correct, reasoning).
                        """)
                .judgePrompt("""
                        User Question: %s
                        AI Response: %s
                        Criteria: %s
                        
                        Evaluate if the response uses the attached file facts described in the criteria.
                        Use submitJudgement(correct, reasoning).
                        """)
                .build();
    }

    @Test
    void testTextAttachmentInUserMessageIsUsedByAi() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation();
            String fileName = "attachment-context-%s.csv".formatted(UUID.randomUUID());
            FileRef fileRef = fileStorage.saveStream(
                    fileName,
                    new ByteArrayInputStream("""
                            client,open_amount,overdue_days,next_action
                            Attachment Test GmbH,12345.67,42,Call finance owner today
                            """.getBytes(StandardCharsets.UTF_8))
            );
            String question = "Summarize the attached file and name the overdue amount, overdue days, and next action.";
            ChatMessage userMessage = aiConversationService.createUserMessage(
                    conversation,
                    question,
                    List.of(),
                    List.of(new PendingAttachmentInput(fileRef, fileName))
            );

            String response = crmAnalyticsService.processUserMessage(userMessage.getId());

            assertThat(response).isNotBlank();
            long userMessageCount = dataManager.loadValue(
                            "select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.type = :type",
                            Long.class)
                    .parameter("conversationId", conversation.getId())
                    .parameter("type", ChatMessageType.USER.getId())
                    .one();
            long assistantMessageCount = dataManager.loadValue(
                            "select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.type = :type",
                            Long.class)
                    .parameter("conversationId", conversation.getId())
                    .parameter("type", ChatMessageType.ASSISTANT.getId())
                    .one();

            assertThat(userMessageCount).isEqualTo(1);
            assertThat(assistantMessageCount).isEqualTo(1);

            llmJudge.evaluateAnswerWithJudge(
                    question,
                    response,
                    """
                            The response should use the CSV attachment facts.
                            It should mention Attachment Test GmbH.
                            It should report the overdue/open amount 12345.67 or an equivalent formatted value.
                            It should report 42 overdue days.
                            It should include the next action: Call finance owner today.
                            """
            );
        });
    }

    @Test
    void testEntityReferencesAndAttachmentInSameUserMessageAreCombined() {
        systemAuthenticator.runWithSystem(() -> {
            String clientName = "Combined Context Client " + UUID.randomUUID().toString().substring(0, 8);
            Client client = entities.client(clientName);
            Category category = entities.category(
                    "Combined Context Category " + UUID.randomUUID(),
                    "combined-" + UUID.randomUUID()
            );
            CategoryItem product = entities.categoryItem(
                    "Combined Context Product " + UUID.randomUUID().toString().substring(0, 8),
                    "combined-product-" + UUID.randomUUID(),
                    category,
                    new BigDecimal("499.00"),
                    UomType.PIECES
            );

            AiConversation conversation = aiConversationService.createNewConversation();
            String fileName = "combined-context-%s.csv".formatted(UUID.randomUUID());
            FileRef fileRef = fileStorage.saveStream(
                    fileName,
                    new ByteArrayInputStream("""
                            metric,value
                            open_invoices,5
                            overdue_amount,9876.54
                            next_action,Call finance owner today
                            """.getBytes(StandardCharsets.UTF_8))
            );

            String question = """
                    Analyse the referenced client and product situation, including the figures from the attached file.
                    Mention the client and the product by name, and use the concrete numbers from the file.
                    """;

            ChatMessage userMessage = aiConversationService.createUserMessage(
                    conversation,
                    question,
                    List.of(
                            idSerialization.idToString(Id.of(client)),
                            idSerialization.idToString(Id.of(product))
                    ),
                    List.of(new PendingAttachmentInput(fileRef, fileName))
            );

            String response = crmAnalyticsService.processUserMessage(userMessage.getId());

            assertThat(response).isNotBlank();
            long userMessageCount = dataManager.loadValue(
                            "select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.type = :type",
                            Long.class)
                    .parameter("conversationId", conversation.getId())
                    .parameter("type", ChatMessageType.USER.getId())
                    .one();
            long assistantMessageCount = dataManager.loadValue(
                            "select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.type = :type",
                            Long.class)
                    .parameter("conversationId", conversation.getId())
                    .parameter("type", ChatMessageType.ASSISTANT.getId())
                    .one();

            assertThat(userMessageCount).isEqualTo(1);
            assertThat(assistantMessageCount).isEqualTo(1);

            llmJudge.evaluateAnswerWithJudge(
                    question,
                    response,
                    """
                            The response should be one coherent analysis (not three separate reactions).
                            It should mention the referenced client by name: %s.
                            It should mention the referenced product by name: %s.
                            It should use at least one concrete number from the CSV attachment, such as 9876.54 or 5 open invoices.
                            """.formatted(clientName, product.getName())
            );
        });
    }
}
