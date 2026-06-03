package com.company.crm.ai.view.timeline;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;

import java.util.List;

public class AiConversationTimelineItemFactory {

    public List<TimelineItem> buildTimelineItems(AiConversation conversation) {
        return conversation.getSortedMessages().stream()
                .map(this::createTimelineItem)
                .toList();
    }

    private TimelineItem createTimelineItem(ChatMessage message) {
        ChatMessageType type = message.getType();
        if (ChatMessageType.ASSISTANT.equals(type) || ChatMessageType.TOOL.equals(type)) {
            return TimelineItem.assistant(message);
        }
        return TimelineItem.user(message);
    }
}
