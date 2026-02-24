package com.company.crm.ai.service;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import io.jmix.core.security.Authenticated;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AiConversationTitleService {

    private static final Logger log = LoggerFactory.getLogger(AiConversationTitleService.class);

    private static final String DEFAULT_TITLE = "New Chat";
    private static final String LEGACY_DEFAULT_TITLE = "AI Chat Session";
    private static final int TITLE_MAX_LENGTH = 80;
    private static final int TITLE_MIN_USER_MESSAGES = 1;
    private static final int TITLE_MAX_CONTEXT_MESSAGES = 6;

    private final DataManager dataManager;
    private final ChatClient chatClient;
    private final AiConversationTitleProperties properties;

    public AiConversationTitleService(
            DataManager dataManager,
            ChatClient.Builder chatClientBuilder,
            AiConversationTitleProperties properties
    ) {
        this.dataManager = dataManager;
        this.properties = properties;
        this.chatClient = chatClientBuilder.clone()
                .defaultSystem(properties.getSystemPrompt())
                .defaultOptions(buildOptions(properties))
                .build();
    }

    @Async
    @Authenticated
    public void generateTitleIfNeededAsync(UUID conversationId) {
        if (conversationId == null) {
            return;
        }
        try {
            AiConversation conversation = dataManager.load(AiConversation.class)
                    .id(conversationId)
                    .optional()
                    .orElse(null);

            if (conversation == null || !isUntitled(conversation.getTitle())) {
                return;
            }

            long userMessageCount = dataManager.loadValue(
                            "select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.type = :userType",
                            Long.class
                    )
                    .parameter("conversationId", conversationId)
                    .parameter("userType", ChatMessageType.USER.getId())
                    .one();

            if (userMessageCount < TITLE_MIN_USER_MESSAGES) {
                return;
            }

            List<ChatMessage> contextMessages = loadContextMessages(conversationId);
            String conversationSnippet = buildConversationSnippet(contextMessages);
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
            if (latestConversation == null || !isUntitled(latestConversation.getTitle())) {
                return;
            }

            latestConversation.setTitle(sanitizedTitle);
            dataManager.save(latestConversation);
        } catch (Exception e) {
            log.warn("Failed to generate conversation title for {}", conversationId, e);
        }
    }

    private List<ChatMessage> loadContextMessages(UUID conversationId) {
        List<ChatMessage> recentMessages = dataManager.load(ChatMessage.class)
                .query("select m from ChatMessage m where m.conversation.id = :conversationId order by m.createdDate desc, m.id desc")
                .parameter("conversationId", conversationId)
                .maxResults(TITLE_MAX_CONTEXT_MESSAGES)
                .list();

        ArrayList<ChatMessage> orderedMessages = new ArrayList<>(recentMessages);
        Collections.reverse(orderedMessages);
        return orderedMessages;
    }

    private String buildConversationSnippet(List<ChatMessage> messages) {
        return messages.stream()
                .filter(message -> message.getType() == ChatMessageType.USER || message.getType() == ChatMessageType.ASSISTANT)
                .map(message -> {
                    String role = message.getType() == ChatMessageType.USER ? "User" : "Assistant";
                    return role + ": " + safeContent(message.getContent());
                })
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String generateTitle(String conversationSnippet) {
        String prompt = """
                Create one short title for this CRM conversation.
                Conversation:
                %s
                """.formatted(conversationSnippet);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private BedrockChatOptions buildOptions(AiConversationTitleProperties properties) {
        return BedrockChatOptions.builder()
                .model(properties.getModelId())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .build();
    }

    private boolean isUntitled(String title) {
        return !StringUtils.hasText(title)
                || DEFAULT_TITLE.equals(title)
                || LEGACY_DEFAULT_TITLE.equals(title);
    }

    private String sanitizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "";
        }
        String sanitized = title
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\"", "")
                .trim();
        if (sanitized.endsWith(".")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).trim();
        }
        if (sanitized.length() > TITLE_MAX_LENGTH) {
            sanitized = sanitized.substring(0, TITLE_MAX_LENGTH).trim();
        }
        return sanitized;
    }

    private String safeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replace("\n", " ").trim();
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }
}
