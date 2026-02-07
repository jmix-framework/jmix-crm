package com.company.crm.app.service.ai;

import com.company.crm.ai.jmix.introspection.EntityListTool;
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
 * Service for AI-powered CRM analytics
 * Provider-agnostic implementation using Spring AI
 */
@Service("crm_CrmAnalyticsService")
public class CrmAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsService.class);

    private final ChatClient chatClient;
    private final JpqlQueryTool jpqlQueryTool;
    private final CrmDomainModelIntrospectionTool crmDomainModelIntrospectionTool;
    private final EntityListTool entityListTool;

    @Autowired
    public CrmAnalyticsService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/crm-system-prompt.st") Resource systemPrompt,
            JpqlQueryTool jpqlQueryTool,
            CrmDomainModelIntrospectionTool crmDomainModelIntrospectionTool,
            EntityListTool entityListTool,
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
        this.entityListTool = entityListTool;

        log.info("CRM Analytics Service initialized with custom ChatClient configuration");
    }

    /**
     * Process natural language business questions with conversation memory
     *
     * @param userQuestion Natural language question about the business
     * @param conversationId Unique identifier for the conversation
     * @return AI-generated insights with data from the CRM system, with memory of previous messages
     */
    public String processBusinessQuestion(String userQuestion, String conversationId) {
        log.info("Processing business question with memory: {} (conversation: {})", userQuestion, conversationId);

        try {

            return chatClient.prompt()
                    .user(userQuestion)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(jpqlQueryTool, crmDomainModelIntrospectionTool, entityListTool)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error processing business question with memory: {} (conversation: {})", userQuestion, conversationId, e);
            return "I encountered an error while analyzing your question: " + e.getMessage() +
                   "\\nPlease try rephrasing your question or check the system configuration.";
        }
    }

}