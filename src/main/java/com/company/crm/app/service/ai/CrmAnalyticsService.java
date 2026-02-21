package com.company.crm.app.service.ai;

import com.company.crm.ai.jmix.introspection.AiDomainModelDescriptorYamlExporter;
import com.company.crm.ai.jmix.introspection.JmixJpaEntityDiscoveryTool;
import com.company.crm.ai.jmix.query.AiJpqlQueryService;
import com.company.crm.ai.jmix.query.JpqlQueryTool;
import com.company.crm.ai.jmix.report.introspection.AiReportModelDescriptorYamlExporter;
import com.company.crm.ai.jmix.report.introspection.JmixReportDiscoveryTool;
import com.company.crm.ai.jmix.report.run.AiReportExecutionService;
import com.company.crm.ai.jmix.report.run.RunReportTool;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemComment;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import io.jmix.core.MetadataTools;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * AI-powered analytics service that processes natural language business questions against CRM data.
 */
@Service("crm_CrmAnalyticsService")
public class CrmAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsService.class);

    private static final Set<Class<?>> CRM_ENTITIES = Set.of(
            Client.class, Order.class, OrderItem.class, Category.class,
            CategoryItem.class, CategoryItemComment.class, Invoice.class,
            Payment.class, User.class, Contact.class
    );

    // Whitelist for reports allowed in this service
    private static final List<String> CRM_REPORTS = List.of("client-360-report", "invoice-report");

    private final ChatClient chatClient;
    
    // Tools are manually instantiated POJOs
    private final JpqlQueryTool jpqlQueryTool;
    private final JmixJpaEntityDiscoveryTool jmixJpaEntityDiscoveryTool;
    private final JmixReportDiscoveryTool jmixReportDiscoveryTool;
    private final RunReportTool runReportTool;

    @Autowired
    public CrmAnalyticsService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/crm-analytics-system-prompt.st") Resource systemPrompt,
            AiJpqlQueryService aiJpqlQueryService,
            AiDomainModelDescriptorYamlExporter entityYamlExporter,
            AiReportModelDescriptorYamlExporter reportYamlExporter,
            AiReportExecutionService aiReportExecutionService,
            MetadataTools metadataTools,
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

        // Instantiate tools with specific whitelists
        this.jpqlQueryTool = new JpqlQueryTool(aiJpqlQueryService);
        this.jmixJpaEntityDiscoveryTool = new JmixJpaEntityDiscoveryTool(metadataTools, entityYamlExporter, CRM_ENTITIES);
        this.jmixReportDiscoveryTool = new JmixReportDiscoveryTool(reportYamlExporter, CRM_REPORTS);
        this.runReportTool = new RunReportTool(aiReportExecutionService, CRM_REPORTS);
    }

    /**
     * Processes a natural language business question and returns AI-generated insights.
     */
    public String processBusinessQuestion(String userQuestion, String conversationId) {
        log.debug("Processing business question: {} (conversation: {})", userQuestion, conversationId);

        try {
            return chatClient.prompt()
                    .user(userQuestion)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(jpqlQueryTool, jmixJpaEntityDiscoveryTool, jmixReportDiscoveryTool, runReportTool)
                    .call()
                    .content();

        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString().substring(0, 8);
            log.error("Error processing business question [Error ID: {}]", errorId, e);
            return "I encountered an error while analyzing your question. Please contact support and provide this Error ID: " + errorId;
        }
    }
}
