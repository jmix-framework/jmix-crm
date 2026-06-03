package com.company.crm.ai.service;

import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.prompt.AiPromptContentBuilder;
import com.company.crm.ai.prompt.AiTextSanitizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiConversationTitlePromptBuilder {

    private static final int MESSAGE_SNIPPET_MAX_LENGTH = 240;

    private final AiTitleProperties titleProperties;

    public AiConversationTitlePromptBuilder(AiTitleProperties titleProperties) {
        this.titleProperties = titleProperties;
    }

    public String buildConversationSnippet(List<ChatMessage> messages) {
        String snippetLines = messages.stream()
                .filter(this::isSnippetMessage)
                .map(this::formatSnippetLine)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));

        return AiPromptContentBuilder.create()
                .appendParagraph(snippetLines)
                .build();
    }

    public String buildTitlePrompt(String conversationSnippet) {
        return AiPromptContentBuilder.create()
                .appendParagraph("Create one short title for this CRM conversation.")
                .appendSection("Conversation", conversationSnippet)
                .build();
    }

    public String sanitizeTitle(String title) {
        return AiTextSanitizer.normalizeSingleLine(title, Integer.MAX_VALUE)
                .map(this::stripTitleDecorations)
                .filter(value -> !isSkipMarker(value))
                .map(value -> AiTextSanitizer.truncate(value, titleProperties.getMaxLength()))
                .orElse("");
    }

    private String stripTitleDecorations(String title) {
        String strippedTitle = title.replace("\"", "");
        return strippedTitle.endsWith(".")
                ? strippedTitle.substring(0, strippedTitle.length() - 1).trim()
                : strippedTitle;
    }

    private boolean isSkipMarker(String title) {
        return AiTitleProperties.SKIP_MARKER.equalsIgnoreCase(title);
    }

    private boolean isSnippetMessage(ChatMessage message) {
        return message.getType() == ChatMessageType.USER || message.getType() == ChatMessageType.ASSISTANT;
    }

    private String formatSnippetLine(ChatMessage message) {
        String role = message.getType().promptLabel();
        return AiTextSanitizer.normalizeSingleLine(message.getContent(), MESSAGE_SNIPPET_MAX_LENGTH)
                .map(content -> role + ": " + content)
                .orElse("");
    }
}
