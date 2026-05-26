package com.company.crm.test.ai.service;

import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.AiConversationTitlePromptBuilder;
import com.company.crm.ai.service.AiTitleProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiConversationTitlePromptBuilderTest {

    @Test
    void buildsSnippetFromUserAndAssistantMessagesOnly() {
        AiConversationTitlePromptBuilder builder = new AiConversationTitlePromptBuilder(new AiTitleProperties());

        String snippet = builder.buildConversationSnippet(List.of(
                message(ChatMessageType.SYSTEM, "System prompt"),
                message(ChatMessageType.USER, "Show revenue\nfor Q1"),
                message(ChatMessageType.ASSISTANT, "Revenue is up."),
                message(ChatMessageType.TOOL, "Tool result")
        ));

        assertThat(snippet).isEqualTo("""
                User: Show revenue for Q1
                Assistant: Revenue is up.""");
    }

    @Test
    void buildsTitlePromptWithConversationSection() {
        AiConversationTitlePromptBuilder builder = new AiConversationTitlePromptBuilder(new AiTitleProperties());

        assertThat(builder.buildTitlePrompt("User: Show revenue"))
                .isEqualTo("""
                        Create one short title for this CRM conversation.

                        Conversation:

                        User: Show revenue""");
    }

    @Test
    void sanitizesGeneratedTitles() {
        AiTitleProperties titleProperties = new AiTitleProperties();
        titleProperties.setMaxLength(12);
        AiConversationTitlePromptBuilder builder = new AiConversationTitlePromptBuilder(titleProperties);

        assertThat(builder.sanitizeTitle("\"Revenue overview.\"")).isEqualTo("Revenue over");
        assertThat(builder.sanitizeTitle("NEW_CONVERSATION")).isEmpty();
        assertThat(builder.sanitizeTitle("\"NEW_CONVERSATION.\"")).isEmpty();
        assertThat(builder.sanitizeTitle("   ")).isEmpty();
    }

    private ChatMessage message(ChatMessageType type, String content) {
        ChatMessage message = new ChatMessage();
        message.setType(type);
        message.setContent(content);
        return message;
    }
}
