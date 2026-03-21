package com.company.crm.ai.service;

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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiConversationTitleService {

    private static final Logger log = LoggerFactory.getLogger(AiConversationTitleService.class);

    private static final String SKIP_TITLE_MARKER = "NEW_CONVERSATION";
    private static final int TITLE_MAX_LENGTH = 80;
    private static final int MESSAGE_SNIPPET_MAX_LENGTH = 240;
    private static final int TITLE_MIN_USER_MESSAGES = 1;
    private static final int TITLE_MAX_CONTEXT_MESSAGES = 6;
    private static final double TITLE_TEMPERATURE = 0.0;
    private static final int TITLE_MAX_TOKENS = 32;

    private final UnconstrainedDataManager dataManager;
    private final ChatClient chatClient;
    private final Messages messages;

    public AiConversationTitleService(
            UnconstrainedDataManager dataManager,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/ai-conversation-title-system-prompt.st") Resource systemPrompt,
            AiConversationTitleProperties properties,
            Messages messages) {
        this.dataManager = dataManager;
        this.chatClient = chatClientBuilder.clone()
                .defaultSystem(systemPrompt)
                .defaultOptions(buildOptions(properties.getModelId()))
                .build();
        this.messages = messages;
    }

    public void generateTitleIfNeeded(UUID conversationId) {
        if (conversationId == null) {
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

    public String generateTitle(String conversationSnippet) {
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

    private OpenAiChatOptions buildOptions(String modelId) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .temperature(TITLE_TEMPERATURE)
                .maxCompletionTokens(TITLE_MAX_TOKENS);
        if (StringUtils.hasText(modelId)) {
            optionsBuilder.model(modelId);
        }
        return optionsBuilder.build();
    }

    private boolean hasAiTitle(String title) {
        return StringUtils.hasText(title)
                && !messages.formatMessage(AiConversation.class, "defaultTitle").equals(title);
    }

    public String sanitizeTitle(String title) {
        return normalize(title, TITLE_MAX_LENGTH)
                .map(t -> t.replaceAll("\"", ""))
                .map(t -> t.endsWith(".") ? t.substring(0, t.length() - 1).trim() : t)
                .filter(t -> !t.contains(SKIP_TITLE_MARKER))
                .orElse("");
    }

    private String safeContent(String content) {
        return normalize(content, MESSAGE_SNIPPET_MAX_LENGTH).orElse("");
    }

    private Optional<String> normalize(String text, int maxLength) {
        return Optional.ofNullable(text)
                .filter(StringUtils::hasText)
                .map(t -> t.replaceAll("[\\n\\r]+", " ").trim())
                .map(t -> t.length() > maxLength ? t.substring(0, maxLength).trim() : t);
    }
}
