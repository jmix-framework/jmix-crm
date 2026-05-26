package com.company.crm.ai.service;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.memory.JmixChatMemoryRepository;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.tool.AiToolStatusPublisher;
import com.company.crm.ai.tool.CrmAiToolFactory;
import com.company.crm.report.CategoryCashflowRiskReport;
import com.company.crm.report.Client360Report;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.security.CurrentAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AI-powered analytics service that processes natural language business questions against CRM data.
 */
@Service
public class CrmAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsService.class);
    private static final List<String> ALLOWED_REPORT_CODES = List.of(
            Client360Report.CODE,
            CategoryCashflowRiskReport.CODE
    );

    private final CrmAiToolFactory crmAiToolFactory;

    private final Resource systemPrompt;
    private final ChatClient statelessChatClient;
    private final JmixChatMemoryRepository chatMemoryRepository;
    private final CurrentAuthentication currentAuthentication;
    private final DataManager dataManager;
    private final AiContextEntityRegistry contextEntityRegistry;

    @Autowired
    public CrmAnalyticsService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/crm-analytics-system-prompt.st") Resource systemPrompt,
            JmixChatMemoryRepository chatMemoryRepository,
            CurrentAuthentication currentAuthentication,
            DataManager dataManager,
            AiContextEntityRegistry contextEntityRegistry,
            CrmAiToolFactory crmAiToolFactory) {
        this.statelessChatClient = chatClientBuilder.clone()
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();
        this.chatMemoryRepository = chatMemoryRepository;
        this.systemPrompt = systemPrompt;
        this.currentAuthentication = currentAuthentication;
        this.dataManager = dataManager;
        this.contextEntityRegistry = contextEntityRegistry;
        this.crmAiToolFactory = crmAiToolFactory;
    }

    public String processUserMessage(UUID chatMessageId) {
        return processUserMessage(chatMessageId, null);
    }

    public String processUserMessage(UUID chatMessageId, Consumer<AiUiStatusUpdate> uiStatusUpdateCallback) {
        ChatMessage userMessage = loadUserMessage(chatMessageId);

        UUID conversationId = userMessage.getConversation().getId();
        String conversationIdString = conversationId.toString();
        List<Message> history = chatMemoryRepository.findByConversationId(conversationIdString);
        ChatMessage assistantMessage = createAssistantPlaceholder(userMessage);
        UUID assistantMessageId = assistantMessage.getId();

        try {
            publishUiStatus(uiStatusUpdateCallback, "Thinking...");

            String response = requestAssistantResponse(conversationId, assistantMessageId, uiStatusUpdateCallback, history);

            saveAssistantResponse(assistantMessage, response);
            return response;
        } catch (RuntimeException e) {
            removeAssistantPlaceholder(assistantMessage);
            throw e;
        }
    }

    private ChatMessage loadUserMessage(UUID chatMessageId) {
        return dataManager.load(ChatMessage.class)
                .id(chatMessageId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                        .add("conversation", FetchPlan.BASE)
                        .add("entityReferences", FetchPlan.BASE)
                        .add("attachments", FetchPlan.BASE))
                .one();
    }

    private ChatMessage createAssistantPlaceholder(ChatMessage userMessage) {
        ChatMessage assistantMessage = dataManager.create(ChatMessage.class);
        assistantMessage.setConversation(userMessage.getConversation());
        assistantMessage.setType(ChatMessageType.ASSISTANT);
        assistantMessage.setContent("");
        return dataManager.save(assistantMessage);
    }

    private String requestAssistantResponse(UUID conversationId,
                                            UUID assistantMessageId,
                                            Consumer<AiUiStatusUpdate> uiStatusUpdateCallback,
                                            List<Message> history) {
        return buildPromptSpec(conversationId, assistantMessageId, uiStatusUpdateCallback)
                .messages(history)
                .call()
                .content();
    }

    private void saveAssistantResponse(ChatMessage assistantMessage, String response) {
        assistantMessage.setContent(response);
        dataManager.save(assistantMessage);
    }

    private void removeAssistantPlaceholder(ChatMessage assistantMessage) {
        try {
            dataManager.remove(assistantMessage);
        } catch (Exception cleanupError) {
            log.warn("Failed to remove assistant placeholder {}", assistantMessage.getId(), cleanupError);
        }
    }

    private ChatClient.ChatClientRequestSpec buildPromptSpec(UUID conversationUuid,
                                                            UUID assistantMessageId,
                                                            Consumer<AiUiStatusUpdate> uiStatusUpdateCallback) {
        ChatClient.ChatClientRequestSpec baseSpec = statelessChatClient.prompt()
                .system(system -> system
                        .text(systemPrompt)
                        .param("responseLanguage", resolveResponseLanguage()))
                .tools(crmAiToolFactory.builder()
                        .jpqlQueryExecutorTool()
                        .viewsDiscoveryTool()
                        .entitiesDiscoveryTool(contextEntityRegistry.aiToolContextEntityDefinitions())
                        .reportsDiscoveryTool(ALLOWED_REPORT_CODES)
                        .runReportTool(ALLOWED_REPORT_CODES)
                        .buildToolsArray());

        Map<String, Object> toolContext = buildToolContext(conversationUuid, assistantMessageId, uiStatusUpdateCallback);
        return toolContext.isEmpty() ? baseSpec : baseSpec.toolContext(toolContext);
    }

    private Map<String, Object> buildToolContext(UUID conversationUuid,
                                                 UUID assistantMessageId,
                                                 Consumer<AiUiStatusUpdate> uiStatusUpdateCallback) {
        Map<String, Object> toolContext = new HashMap<>();
        putIfPresent(toolContext, "conversationId", conversationUuid);
        putIfPresent(toolContext, "assistantMessageId", assistantMessageId);
        putIfPresent(toolContext, AiToolStatusPublisher.STATUS_UPDATE_CALLBACK, uiStatusUpdateCallback);
        return toolContext;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        Optional.ofNullable(value).ifPresent(presentValue -> target.put(key, presentValue));
    }

    private void publishUiStatus(Consumer<AiUiStatusUpdate> uiStatusUpdateCallback, String message) {
        Optional.ofNullable(uiStatusUpdateCallback)
                .ifPresent(callback -> callback.accept(new AiUiStatusUpdate(message)));
    }

    private String resolveResponseLanguage() {
        return currentAuthentication.getLocale().getLanguage();
    }
}
