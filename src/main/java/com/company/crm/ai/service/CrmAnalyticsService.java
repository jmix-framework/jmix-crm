package com.company.crm.ai.service;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.memory.JmixChatMemoryRepository;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.tool.AiToolUiStatus;
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
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AI-powered analytics service that processes natural language business questions against CRM data.
 */
@Service
public class CrmAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsService.class);

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
        ChatMessage userMessage = dataManager.load(ChatMessage.class)
                .id(chatMessageId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                        .add("conversation", FetchPlan.BASE)
                        .add("entityReferences", FetchPlan.BASE)
                        .add("attachments", FetchPlan.BASE))
                .one();

        UUID conversationId = userMessage.getConversation().getId();
        String conversationIdString = conversationId.toString();
        List<Message> history = chatMemoryRepository.findByConversationId(conversationIdString);

        // TODO: reassign local variable
        ChatMessage assistantMessage = dataManager.create(ChatMessage.class);
        assistantMessage.setConversation(userMessage.getConversation());
        assistantMessage.setType(ChatMessageType.ASSISTANT);
        assistantMessage.setContent("");
        assistantMessage = dataManager.save(assistantMessage);
        UUID assistantMessageId = assistantMessage.getId();

        try {
            publishUiStatus(uiStatusUpdateCallback, "Thinking...");

            String response = buildPromptSpec(statelessChatClient, conversationId, assistantMessageId, uiStatusUpdateCallback)
                    .messages(history)
                    .call()
                    .content();

            // TODO: immer dieses != null ? .. : ..... sowas macht kein echter programmierer. einfach null reingeben, fertig
            assistantMessage.setContent(response != null ? response : "");
            dataManager.save(assistantMessage);
            return response;
        } catch (RuntimeException e) {
            try {
                // TODO: warum? das save ist die letzte operation. wenn sie funktioniert, kommt man hier nicht rein, wenn nicht (und exception) dann wurde garnicht gespeichert...
                dataManager.remove(assistantMessage);
            } catch (Exception cleanupError) {
                log.warn("Failed to remove assistant placeholder {}", assistantMessage.getId(), cleanupError);
            }
            throw e;
        }
    }

    // TODO: vielleicht eigene klasse?
    private ChatClient.ChatClientRequestSpec buildPromptSpec(ChatClient client,
                                                            UUID conversationUuid,
                                                            UUID assistantMessageId,
                                                            Consumer<AiUiStatusUpdate> uiStatusUpdateCallback) {
        List<String> allowedReports = List.of(
                Client360Report.CODE,
                CategoryCashflowRiskReport.CODE
        );

        Map<String, Object> toolContext = new HashMap<>();
        if (conversationUuid != null) {
            toolContext.put("conversationId", conversationUuid);
        }
        if (assistantMessageId != null) {
            toolContext.put("assistantMessageId", assistantMessageId);
        }
        if (uiStatusUpdateCallback != null) {
            toolContext.put(AiToolUiStatus.UI_STATUS_UPDATE_CALLBACK, uiStatusUpdateCallback);
        }

        // TODO: Reassign variable
        ChatClient.ChatClientRequestSpec spec = client.prompt()
                .system(system -> system
                        .text(systemPrompt)
                        .param("responseLanguage", resolveResponseLanguage()))
                .tools(crmAiToolFactory.builder()
                        .jpqlQueryExecutorTool()
                        .viewsDiscoveryTool()
                        .entitiesDiscoveryTool(contextEntityRegistry.toolEntityClasses())
                        .reportsDiscoveryTool(allowedReports)
                        .runReportTool(allowedReports)
                        .buildToolsArray());
        if (!toolContext.isEmpty()) {
            spec = spec.toolContext(toolContext);
        }
        return spec;
    }

    private void publishUiStatus(Consumer<AiUiStatusUpdate> uiStatusUpdateCallback, String message) {
        // TODO: nicht überall if checks....
        if (uiStatusUpdateCallback != null) {
            uiStatusUpdateCallback.accept(new AiUiStatusUpdate(message));
        }
    }

    private String resolveResponseLanguage() {
        return currentAuthentication.getLocale().getLanguage();
    }
}
