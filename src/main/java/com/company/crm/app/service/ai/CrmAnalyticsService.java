package com.company.crm.app.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Autowired
    public CrmAnalyticsService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/crm-system-prompt.st") Resource systemPrompt,
            JpqlQueryTool jpqlQueryTool
    ) {
        // Build use-case specific ChatClient with CRM system prompt and logging
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        // Store tools for explicit use in queries
        this.jpqlQueryTool = jpqlQueryTool;

        log.info("CRM Analytics Service initialized with custom ChatClient configuration");
    }

    /**
     * Process natural language business questions and provide AI-powered insights
     *
     * @param userQuestion Natural language question about the business
     * @return AI-generated insights with data from the CRM system
     */
    public String processBusinessQuestion(String userQuestion) {
        log.info("Processing business question: {}", userQuestion);

        try {
            UserMessage userMessage = new UserMessage(userQuestion);
            Prompt prompt = new Prompt(userMessage);

            String answer = chatClient.prompt(prompt)
                    .tools(jpqlQueryTool)
                    .call()
                    .content();

            log.info("Generated analytics response for question: {}", userQuestion);
            log.debug("Response length: {} characters", answer.length());

            return answer;

        } catch (Exception e) {
            log.error("Error processing business question: {}", userQuestion, e);
            return "I encountered an error while analyzing your question: " + e.getMessage() +
                   "\\nPlease try rephrasing your question or check the system configuration.";
        }
    }

    /**
     * Health check for the AI service
     *
     * @return Simple response to verify the service is working
     */
    public String healthCheck() {
        try {
            String response = processBusinessQuestion("Please respond with 'CRM Analytics Service is operational' to confirm the system is working.");
            log.info("Health check successful");
            return response;
        } catch (Exception e) {
            log.error("Health check failed", e);
            return "Health check failed: " + e.getMessage();
        }
    }
}