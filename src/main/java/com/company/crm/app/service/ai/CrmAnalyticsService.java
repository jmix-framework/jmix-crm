package com.company.crm.app.service.ai;

import com.company.crm.ai.entity.AiConversationAttachment;
import com.company.crm.ai.service.AiConversationTitleService;
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
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.MetadataTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.api.BedrockMediaFormat;
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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final List<String> CRM_REPORTS = List.of(
            "client-360-report",
            "invoice-report",
            "category-cashflow-risk-report"
    );

    private static final String CRM_MESSAGE_TYPE_METADATA_KEY = "crmMessageType";
    private static final String ATTACHMENT_MESSAGE_TYPE = "ATTACHMENT";

    private final ChatClient chatClient;

    // Tools are manually instantiated POJOs
    private final JpqlQueryTool jpqlQueryTool;
    private final JmixJpaEntityDiscoveryTool jmixJpaEntityDiscoveryTool;
    private final JmixReportDiscoveryTool jmixReportDiscoveryTool;
    private final RunReportTool runReportTool;
    private final DataManager dataManager;
    private final FileStorageLocator fileStorageLocator;
    private final AiConversationTitleService aiConversationTitleService;

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
            DataManager dataManager,
            FileStorageLocator fileStorageLocator,
            AiConversationTitleService aiConversationTitleService
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
        this.dataManager = dataManager;
        this.fileStorageLocator = fileStorageLocator;
        this.aiConversationTitleService = aiConversationTitleService;
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
        String safeFileName = StringUtils.hasText(fileName) ? fileName : "file";
        String safeActorName = StringUtils.hasText(actorName) ? actorName : "User";
        String uploadEventPrompt = String.format("%s added attachment \"%s\"", safeActorName, safeFileName);
        return processBusinessQuestionInternal(
                uploadEventPrompt,
                conversationId,
                List.of(new AttachmentPromptReference(attachmentId, mimeType)),
                Map.of(CRM_MESSAGE_TYPE_METADATA_KEY, ATTACHMENT_MESSAGE_TYPE)
        );
    }

    private String processBusinessQuestionInternal(
            String userQuestion,
            String conversationId,
            List<AttachmentPromptReference> attachmentRefs,
            Map<String, Object> userMetadata
    ) {
        log.debug("Processing business question: {} (conversation: {}, attachmentCount: {})",
                userQuestion, conversationId, attachmentRefs.size());

        UUID conversationUuid = tryParseConversationId(conversationId);
        List<Media> mediaAttachments = resolveMediaAttachments(conversationUuid, attachmentRefs);

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

        if (conversationUuid != null) {
            aiConversationTitleService.generateTitleIfNeededAsync(conversationUuid);
        }
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

    private List<Media> resolveMediaAttachments(UUID conversationUuid, List<AttachmentPromptReference> attachmentRefs) {
        if (attachmentRefs.isEmpty()) {
            return List.of();
        }
        if (conversationUuid == null) {
            throw new IllegalArgumentException("Attachment media input requires a valid conversation UUID.");
        }

        List<Media> mediaList = new ArrayList<>();
        for (AttachmentPromptReference ref : attachmentRefs) {
            AiConversationAttachment attachment = dataManager.load(AiConversationAttachment.class)
                    .query("select e from AiConversationAttachment e where e.id = :id and e.conversation.id = :conversationId")
                    .parameter("id", ref.attachmentId())
                    .parameter("conversationId", conversationUuid)
                    .optional()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Attachment not found in conversation: " + ref.attachmentId()));

            FileRef fileRef = attachment.getFile();
            if (fileRef == null) {
                throw new IllegalArgumentException("Attachment has no file payload: " + ref.attachmentId());
            }

            MimeType mimeType = resolveSupportedMimeType(ref.mimeType(), attachment.getFileName());
            byte[] fileBytes = readFileBytes(fileRef);
            String mediaName = sanitizeMediaName(attachment.getFileName());

            Media media = Media.builder()
                    .mimeType(mimeType)
                    .data(fileBytes)
                    .name(mediaName)
                    .build();
            mediaList.add(media);
        }

        return mediaList;
    }

    private byte[] readFileBytes(FileRef fileRef) {
        try (InputStream inputStream = resolveFileStorage(fileRef).openStream(fileRef)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read attachment from file storage: " + fileRef.getFileName(), e);
        }
    }

    private FileStorage resolveFileStorage(FileRef fileRef) {
        String storageName = fileRef.getStorageName();
        if (!StringUtils.hasText(storageName)) {
            throw new IllegalStateException("Attachment FileRef has no storage name: " + fileRef.getFileName());
        }
        return fileStorageLocator.getByName(storageName);
    }

    private MimeType resolveSupportedMimeType(String rawMimeType, String fileName) {
        MimeType parsedMimeType = tryParseMimeType(rawMimeType);
        if (parsedMimeType != null && isSupportedByBedrock(parsedMimeType)) {
            return parsedMimeType;
        }

        MimeType extensionMimeType = mimeTypeFromExtension(fileName);
        if (extensionMimeType != null && isSupportedByBedrock(extensionMimeType)) {
            return extensionMimeType;
        }

        throw new IllegalArgumentException("Unsupported attachment media type for model input: " + fileName);
    }

    private MimeType tryParseMimeType(String rawMimeType) {
        if (!StringUtils.hasText(rawMimeType)) {
            return null;
        }
        try {
            return MimeTypeUtils.parseMimeType(rawMimeType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private MimeType mimeTypeFromExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }

        String normalized = fileName.toLowerCase();
        if (normalized.endsWith(".pdf")) return Media.Format.DOC_PDF;
        if (normalized.endsWith(".csv")) return Media.Format.DOC_CSV;
        if (normalized.endsWith(".doc")) return Media.Format.DOC_DOC;
        if (normalized.endsWith(".docx")) return Media.Format.DOC_DOCX;
        if (normalized.endsWith(".xls")) return Media.Format.DOC_XLS;
        if (normalized.endsWith(".xlsx")) return Media.Format.DOC_XLSX;
        if (normalized.endsWith(".html") || normalized.endsWith(".htm")) return Media.Format.DOC_HTML;
        if (normalized.endsWith(".txt")) return Media.Format.DOC_TXT;
        if (normalized.endsWith(".md")) return Media.Format.DOC_MD;
        if (normalized.endsWith(".png")) return Media.Format.IMAGE_PNG;
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) return Media.Format.IMAGE_JPEG;
        if (normalized.endsWith(".gif")) return Media.Format.IMAGE_GIF;
        if (normalized.endsWith(".webp")) return Media.Format.IMAGE_WEBP;
        return null;
    }

    private boolean isSupportedByBedrock(MimeType mimeType) {
        return BedrockMediaFormat.isSupportedDocumentFormat(mimeType)
                || BedrockMediaFormat.isSupportedImageFormat(mimeType)
                || BedrockMediaFormat.isSupportedVideoFormat(mimeType);
    }

    private String sanitizeMediaName(String fileName) {
        String candidate = StringUtils.hasText(fileName) ? fileName : "uploaded-file";
        String sanitized = candidate
                .replaceAll("[^A-Za-z0-9\\s\\-()\\[\\]]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        if (!StringUtils.hasText(sanitized)) {
            sanitized = "uploaded-file";
        }

        return sanitized.length() > 96 ? sanitized.substring(0, 96) : sanitized;
    }

    private record AttachmentPromptReference(UUID attachmentId, String mimeType) {
    }
}
