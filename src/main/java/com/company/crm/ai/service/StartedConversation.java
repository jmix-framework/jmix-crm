package com.company.crm.ai.service;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;

public record StartedConversation(AiConversation conversation,
                                  ChatMessage firstUserMessage) {
}
