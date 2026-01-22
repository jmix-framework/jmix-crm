package com.company.crm.view.component.aiconversation;

/**
 * Request object containing the user message and conversation context.
 * Used for passing conversation data between different message processing components.
 *
 * @param userMessage    the message text entered by the user
 * @param conversationId the unique identifier of the conversation
 */
public record ConversationRequest(String userMessage, String conversationId) {
}