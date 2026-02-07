package com.company.crm.app.service.ai;

import com.company.crm.ai.jmix.introspection.JmixJpaEntityDiscoveryTool;
import com.company.crm.ai.jmix.query.JpqlQueryTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * AI-powered analytics service that processes natural language business questions against CRM data.
 *
 * <p>This service leverages Spring AI's ChatClient with function calling capabilities to:
 * <ul>
 *   <li>Understand natural language business questions</li>
 *   <li>Generate and execute JPQL queries against the CRM database</li>
 *   <li>Introspect the CRM domain model for context</li>
 *   <li>Maintain conversation memory for contextual multi-turn interactions</li>
 * </ul>
 *
 * <p>The service is configured with a system prompt that defines its role and capabilities,
 * and uses three main tools: JPQL query execution, domain model introspection, and JPA entity discovery.
 */
@Service("crm_CrmAnalyticsService")
public class CrmAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsService.class);

    private final ChatClient chatClient;
    private final JpqlQueryTool jpqlQueryTool;
    private final CrmDomainModelIntrospectionTool crmDomainModelIntrospectionTool;
    private final JmixJpaEntityDiscoveryTool jmixJpaEntityDiscoveryTool;

    @Autowired
    public CrmAnalyticsService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/crm-analytics-system-prompt.st") Resource systemPrompt,
            JpqlQueryTool jpqlQueryTool,
            CrmDomainModelIntrospectionTool crmDomainModelIntrospectionTool,
            JmixJpaEntityDiscoveryTool jmixJpaEntityDiscoveryTool,
            ChatMemoryRepository chatMemoryRepository
    ) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .build();

        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();

        this.jpqlQueryTool = jpqlQueryTool;
        this.crmDomainModelIntrospectionTool = crmDomainModelIntrospectionTool;
        this.jmixJpaEntityDiscoveryTool = jmixJpaEntityDiscoveryTool;
    }

    /**
     * Processes a natural language business question and returns AI-generated insights.
     *
     * <p>The question is processed using AI function calling to query the CRM database and
     * introspect the domain model as needed. Conversation memory is maintained using the
     * provided conversation ID to enable contextual multi-turn interactions.
     *
     * @param userQuestion the natural language business question to process
     * @param conversationId unique identifier for maintaining conversation context
     * @return AI-generated response with insights based on CRM data, or error message if processing fails
     */
    public String processBusinessQuestion(String userQuestion, String conversationId) {
        log.info("Processing business question with memory: {} (conversation: {})", userQuestion, conversationId);

        try {

            return chatClient.prompt()
                    .user(userQuestion)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(jpqlQueryTool, crmDomainModelIntrospectionTool, jmixJpaEntityDiscoveryTool)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error processing business question with memory: {} (conversation: {})", userQuestion, conversationId, e);
            return "I encountered an error while analyzing your question: " + e.getMessage() +
                   "\\nPlease try rephrasing your question or check the system configuration.";
        }
    }

}