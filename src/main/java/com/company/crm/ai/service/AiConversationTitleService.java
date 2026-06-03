package com.company.crm.ai.service;

import com.company.crm.ai.config.CrmAiConfig;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.Messages;
import io.jmix.core.UnconstrainedDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AiConversationTitleService {

    private static final Logger log = LoggerFactory.getLogger(AiConversationTitleService.class);

    private static final double TITLE_TEMPERATURE = 0.0;
    private static final int TITLE_MAX_TOKENS = 32;

    private final UnconstrainedDataManager dataManager;
    private final ChatClient chatClient;
    private final Messages messages;
    private final CrmAiConfig crmAiConfig;
    private final AiTitleProperties titleProperties;
    private final AiConversationTitlePromptBuilder titlePromptBuilder;

    public AiConversationTitleService(
            UnconstrainedDataManager dataManager,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/ai-conversation-title-system-prompt.st") Resource systemPrompt,
            AiTitleProperties titleProperties,
            AiConversationTitlePromptBuilder titlePromptBuilder,
            AiSmallModelProperties smallModelProperties,
            CrmAiConfig crmAiConfig,
            Messages messages) {
        this.dataManager = dataManager;
        this.crmAiConfig = crmAiConfig;
        this.titleProperties = titleProperties;
        this.titlePromptBuilder = titlePromptBuilder;

        this.chatClient = chatClientBuilder.clone()
                .defaultSystem(renderSystemPrompt(systemPrompt, titleProperties))
                .defaultOptions(buildTitleOptions(smallModelProperties))
                .build();
        this.messages = messages;
    }

    public void generateTitleIfNeeded(UUID conversationId) {
        if (conversationId == null || !crmAiConfig.isAiIntegrationEnabled() || !isEnabled()) {
            return;
        }
        try {
            AiConversation conversation = dataManager.load(AiConversation.class)
                    .id(conversationId)
                    .optional()
                    .orElse(null);

            if (conversation == null || hasAiTitle(conversation.getTitle())) {
                return;
            }

            long userMessageCount = dataManager.loadValue(
                            "select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.type = :userType",
                            Long.class
                    )
                    .parameter("conversationId", conversationId)
                    .parameter("userType", ChatMessageType.USER.getId())
                    .one();

            if (userMessageCount < titleProperties.getMinUserMessages()) {
                return;
            }

            List<ChatMessage> contextMessages = loadContextMessages(conversationId);
            String conversationSnippet = titlePromptBuilder.buildConversationSnippet(contextMessages);
            if (!StringUtils.hasText(conversationSnippet)) {
                return;
            }

            String rawTitle = generateTitle(conversationSnippet);
            String sanitizedTitle = sanitizeTitle(rawTitle);
            if (!StringUtils.hasText(sanitizedTitle)) {
                return;
            }

            AiConversation latestConversation = dataManager.load(AiConversation.class)
                    .id(conversationId)
                    .optional()
                    .orElse(null);
            if (latestConversation == null || hasAiTitle(latestConversation.getTitle())) {
                return;
            }

            latestConversation.setTitle(sanitizedTitle);
            dataManager.save(latestConversation);
            log.debug("Generated title '{}' for conversation {}", sanitizedTitle, conversationId);
        } catch (Exception e) {
            log.warn("Failed to generate conversation title for {}", conversationId, e);
        }
    }

    private List<ChatMessage> loadContextMessages(UUID conversationId) {
        List<ChatMessage> recentMessages = dataManager.load(ChatMessage.class)
                .query("select m from ChatMessage m where m.conversation.id = :conversationId order by m.createdDate desc, m.id desc")
                .parameter("conversationId", conversationId)
                .maxResults(titleProperties.getMaxContextMessages())
                .list();

        ArrayList<ChatMessage> orderedMessages = new ArrayList<>(recentMessages);
        Collections.reverse(orderedMessages);
        return orderedMessages;
    }

    public String generateTitle(String conversationSnippet) {
        return chatClient.prompt()
                .user(titlePromptBuilder.buildTitlePrompt(conversationSnippet))
                .call()
                .content();
    }

    private boolean isEnabled() {
        return titleProperties.isEnabled();
    }

    private boolean hasAiTitle(String title) {
        return StringUtils.hasText(title)
                && !messages.formatMessage(AiConversation.class, "defaultTitle").equals(title);
    }

    public String sanitizeTitle(String title) {
        return titlePromptBuilder.sanitizeTitle(title);
    }

    static OpenAiChatOptions buildTitleOptions(AiSmallModelProperties smallModelProperties) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .temperature(TITLE_TEMPERATURE)
                .maxCompletionTokens(TITLE_MAX_TOKENS);
        if (StringUtils.hasText(smallModelProperties.getModelId())) {
            optionsBuilder.model(smallModelProperties.getModelId());
        }
        return optionsBuilder.build();
    }

    static String renderSystemPrompt(Resource systemPrompt, AiTitleProperties titleProperties) {
        try {
            return StreamUtils.copyToString(systemPrompt.getInputStream(), StandardCharsets.UTF_8)
                    .replace("{skipMarker}", AiTitleProperties.SKIP_MARKER);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read AI conversation title system prompt", e);
        }
    }
}
