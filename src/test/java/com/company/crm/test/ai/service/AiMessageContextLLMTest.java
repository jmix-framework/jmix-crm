package com.company.crm.test.ai.service;

import com.company.crm.AbstractAiTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.CrmAnalyticsService;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.util.ai.LLMJudge;
import com.company.crm.util.ai.LLMJudgeBuilder;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional LLM-based test to verify that ChatMessage entity references are correctly
 * picked up by the AI as context for subsequent questions.
 */
public class AiMessageContextLLMTest extends AbstractAiTest {

    @Autowired
    private CrmAnalyticsService crmAnalyticsService;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private IdSerialization idSerialization;

    @Autowired
    private LLMJudgeBuilder llmJudgeBuilder;

    private LLMJudge llmJudge;

    @BeforeEach
    @Override
    protected void beforeEach() {
        this.llmJudge = llmJudgeBuilder
                .systemPrompt("You are an LLM Judge. Evaluate if the AI response correctly identifies the client name from the provided context.")
                .judgePrompt("""
                        User Question: %s
                        AI Response: %s
                        Criteria: %s
                        
                        Evaluate if the AI response meets the criteria based on the chat history context described in the criteria.
                        Use submitJudgement(correct, reasoning).
                        """)
                .build();
    }

    @Test
    void testEntityReferenceContextIsUsedByAi() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            String clientName = "Context Test Client " + UUID.randomUUID().toString().substring(0, 8);
            Client client = entities.client(clientName);
            String clientRef = idSerialization.idToString(Id.of(client));

            AiConversation conversation = aiConversationService.createNewConversation();

            // when - Creating a message with entity reference context (simulating batch submission)
            ChatMessage contextMessage = dataManager.create(ChatMessage.class);
            contextMessage.setConversation(conversation);
            contextMessage.setType(ChatMessageType.USER);
            contextMessage.setContent("I am attaching a client for your reference.");
            contextMessage.setCreatedDate(OffsetDateTime.now().minusSeconds(5));
            dataManager.save(contextMessage);

            ChatMessageEntityReference ref = dataManager.create(ChatMessageEntityReference.class);
            ref.setMessage(contextMessage);
            ref.setEntityReference(clientRef);
            dataManager.save(ref);

            // Ask follow-up question
            String question = "What is the name of the client I just attached to this conversation?";
            ChatMessage questionMessage = dataManager.create(ChatMessage.class);
            questionMessage.setConversation(conversation);
            questionMessage.setType(ChatMessageType.USER);
            questionMessage.setContent(question);
            questionMessage.setCreatedDate(OffsetDateTime.now());
            dataManager.save(questionMessage);

            // Process the question message
            String response = crmAnalyticsService.processUserMessage(questionMessage.getId());

            // then
            assertThat(response).isNotEmpty();
            System.out.println("AI Response: " + response);

            llmJudge.evaluateAnswerWithJudge(
                    question,
                    response,
                    String.format("The response should explicitly mention that the client name is '%s'.", clientName)
            );
        });
    }

    @Test
    void testMultipleEntityReferencesInOneUserMessageAreUsedByAi() {
        systemAuthenticator.runWithSystem(() -> {
            String clientName = "Multi Context Client " + UUID.randomUUID().toString().substring(0, 8);
            Client client = entities.client(clientName);
            Category category = entities.category("Security Products " + UUID.randomUUID(), "security-" + UUID.randomUUID());
            CategoryItem iamProduct = entities.categoryItem(
                    "Identity Access Management " + UUID.randomUUID().toString().substring(0, 8),
                    "iam-" + UUID.randomUUID(),
                    category,
                    new BigDecimal("1200.00"),
                    UomType.PIECES
            );
            CategoryItem scannerProduct = entities.categoryItem(
                    "Document Scanner Pro " + UUID.randomUUID().toString().substring(0, 8),
                    "scanner-" + UUID.randomUUID(),
                    category,
                    new BigDecimal("350.00"),
                    UomType.PIECES
            );

            AiConversation conversation = aiConversationService.createNewConversation();
            String question = """
                    Please assess which referenced product is a better fit for the referenced client.
                    Mention the client and compare the two referenced products.
                    """;
            ChatMessage userMessage = aiConversationService.createUserMessage(
                    conversation,
                    question,
                    List.of(
                            idSerialization.idToString(Id.of(client)),
                            idSerialization.idToString(Id.of(iamProduct)),
                            idSerialization.idToString(Id.of(scannerProduct))
                    ),
                    List.of()
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
                            The response should use one single turn with all referenced context.
                            It should mention the referenced client by name: %s.
                            It should consider both referenced products: %s and %s.
                            It should compare or prioritize the products for that client.
                            """.formatted(clientName, iamProduct.getName(), scannerProduct.getName())
            );
        });
    }
}
