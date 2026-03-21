package com.company.crm.ai.service;

import com.company.crm.ai.jpql.introspection.exporter.AiDomainModelDescriptorYamlExporter;
import com.company.crm.ai.jpql.introspection.JmixJpaEntityDiscoveryTool;
import com.company.crm.ai.jpql.query.AiJpqlQueryService;
import com.company.crm.ai.jpql.query.JpqlQueryTool;
import com.company.crm.ai.report.introspection.AiReportModelDescriptorYamlExporter;
import com.company.crm.ai.report.introspection.JmixReportDiscoveryTool;
import com.company.crm.ai.report.run.AiReportExecutionService;
import com.company.crm.ai.report.run.RunReportTool;
import com.company.crm.ai.service.AiAttachmentMediaResolver.AttachmentRef;
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
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AI-powered analytics service that processes natural language business questions against CRM data.
 */
@Service
public class CrmAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsService.class);

    private static final Set<Class<?>> CRM_ENTITIES = Set.of(
            Client.class, Order.class, OrderItem.class, Category.class,
            CategoryItem.class, CategoryItemComment.class, Invoice.class,
            Payment.class, User.class, Contact.class
    );

    // Whitelist for reports allowed in this service
    private static final List<String> CRM_REPORTS = List.of(
            "client-360-report",
            "category-cashflow-risk-report"
    );

    private static final String CRM_MESSAGE_TYPE_METADATA_KEY = "crmMessageType";
    private static final String ATTACHMENT_MESSAGE_TYPE = "ATTACHMENT";

    private final ChatClient chatClient;
    private final JpqlQueryTool jpqlQueryTool;
    private final JmixJpaEntityDiscoveryTool jmixJpaEntityDiscoveryTool;
    private final JmixReportDiscoveryTool jmixReportDiscoveryTool;
    private final RunReportTool runReportTool;
    private final AiAttachmentMediaResolver attachmentMediaResolver;

    private final Messages messages;

    @Autowired
    public CrmAnalyticsService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/crm-analytics-system-prompt.st") Resource systemPrompt,
            AiJpqlQueryService aiJpqlQueryService,
            AiDomainModelDescriptorYamlExporter entityYamlExporter,
            AiReportModelDescriptorYamlExporter reportYamlExporter,
            AiReportExecutionService aiReportExecutionService,
            MetadataTools metadataTools,
            ChatMemoryRepository chatMemoryRepository,
            AiAttachmentMediaResolver attachmentMediaResolver,
            Messages messages
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

        this.jpqlQueryTool = new JpqlQueryTool(aiJpqlQueryService);
        this.jmixJpaEntityDiscoveryTool = new JmixJpaEntityDiscoveryTool(metadataTools, entityYamlExporter, CRM_ENTITIES);
        this.jmixReportDiscoveryTool = new JmixReportDiscoveryTool(reportYamlExporter, CRM_REPORTS);
        this.runReportTool = new RunReportTool(aiReportExecutionService, CRM_REPORTS);
        this.attachmentMediaResolver = attachmentMediaResolver;
        this.messages = messages;
    }

    /**
     * Processes a natural language business question and returns AI-generated insights.
     */
    public String processBusinessQuestion(String userQuestion, String conversationId) {
        return processBusinessQuestionInternal(userQuestion, conversationId, List.of(), Map.of());
    }

    /**
     * Sends a dedicated user-upload event as a separate user turn and forwards the uploaded
     * attachment as model media input.
     */
    public String processAttachmentUpload(String conversationId, UUID attachmentId, String fileName, String mimeType, String actorName) {
        String safeFileName = StringUtils.hasText(fileName) ? fileName : messages.formatMessage(CrmAnalyticsService.class, "defaultFileName");
        String safeActorName = StringUtils.hasText(actorName) ? actorName : messages.formatMessage(CrmAnalyticsService.class, "defaultActorName");
        String uploadEventPrompt = messages.formatMessage(CrmAnalyticsService.class, "attachmentUploadPrompt", safeActorName, safeFileName);
        return processBusinessQuestionInternal(
                uploadEventPrompt,
                conversationId,
                List.of(new AttachmentRef(attachmentId, mimeType)),
                Map.of(CRM_MESSAGE_TYPE_METADATA_KEY, ATTACHMENT_MESSAGE_TYPE)
        );
    }

    private String processBusinessQuestionInternal(
            String userQuestion,
            String conversationId,
            List<AttachmentRef> attachmentRefs,
            Map<String, Object> userMetadata
    ) {
        log.debug("Processing business question: {} (conversation: {}, attachmentCount: {})",
                userQuestion, conversationId, attachmentRefs.size());

        UUID conversationUuid = tryParseConversationId(conversationId);
        List<Media> mediaAttachments = attachmentMediaResolver.resolve(conversationUuid, attachmentRefs);

        var promptSpec = chatClient.prompt()
                .user(user -> {
                    user.text(userQuestion);
                    if (!userMetadata.isEmpty()) {
                        user.metadata(userMetadata);
                    }
                    if (!mediaAttachments.isEmpty()) {
                        user.media(mediaAttachments.toArray(new Media[0]));
                    }
                })
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));

        if (conversationUuid != null) {
            promptSpec = promptSpec.toolContext(Map.of("conversationId", conversationUuid));
        }

        String response = promptSpec
                .tools(jpqlQueryTool, jmixJpaEntityDiscoveryTool, jmixReportDiscoveryTool, runReportTool)
                .call()
                .content();

        return response;
    }

    private UUID tryParseConversationId(String conversationId) {
        try {
            return UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid conversationId format for tool context: {}", conversationId);
            return null;
        }
    }
}
