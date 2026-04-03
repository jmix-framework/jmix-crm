package com.company.crm.ai.service;

import com.company.crm.ai.service.AiAttachmentMediaResolver.AttachmentRef;
import com.company.crm.ai.tool.CrmAiTools;
import com.company.crm.model.base.UuidEntity;
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
import io.jmix.core.security.CurrentAuthentication;
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
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AI-powered analytics service that processes natural language business questions against CRM data.
 */
@Service
public class CrmAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsService.class);

    private static final String CRM_MESSAGE_TYPE_METADATA_KEY = "crmMessageType";
    private static final String ATTACHMENT_MESSAGE_TYPE = "ATTACHMENT";
    private final ApplicationContext applicationContext;

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    private final ChatClient chatClient;
    private final AiAttachmentMediaResolver attachmentMediaResolver;
    private final Resource systemPrompt;
    private final Messages messages;
    private final CurrentAuthentication currentAuthentication;

    @Autowired
    public CrmAnalyticsService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/crm-analytics-system-prompt.st") Resource systemPrompt,
            ChatMemoryRepository chatMemoryRepository,
            AiAttachmentMediaResolver attachmentMediaResolver,
            Messages messages,
            CurrentAuthentication currentAuthentication,
            ApplicationContext applicationContext) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
        this.attachmentMediaResolver = attachmentMediaResolver;
        this.systemPrompt = systemPrompt;
        this.messages = messages;
        this.currentAuthentication = currentAuthentication;
        this.applicationContext = applicationContext;
    }

    public boolean isAiIntegrationActive() {
        return openAiApiKey != null
                && !openAiApiKey.isBlank()
                && !openAiApiKey.contains("YOUR_API_KEY");
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
                .system(system -> system
                        .text(systemPrompt)
                        .param("responseLanguage", resolveResponseLanguage()))
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

        Set<Class<? extends UuidEntity>> allowedEntities = Set.of(
                Client.class, Order.class, OrderItem.class, Category.class,
                CategoryItem.class, CategoryItemComment.class, Invoice.class,
                Payment.class, User.class, Contact.class
        );

        List<String> allowedReports = List.of(
                "client-360-report",
                "category-cashflow-risk-report"
        );

        return promptSpec
                .tools(CrmAiTools.builder(applicationContext)
                        .jpqlQueryExecutorTool()
                        .viewsDiscoveryTool()
                        .entitiesDiscoveryTool(allowedEntities)
                        .reportsDiscoveryTool(allowedReports)
                        .runReportTool(allowedReports)
                        .buildToolsArray())
                .call()
                .content();
    }

    private String resolveResponseLanguage() {
        Locale locale = currentAuthentication.getLocale();
        return locale != null ? locale.getLanguage() : Locale.ENGLISH.getLanguage();
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
