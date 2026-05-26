package com.company.crm.ai.view.timeline;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AiConversationTimelineItemFactory {

    public List<TimelineItem> buildTimelineItems(AiConversation conversation) {
        return Optional.ofNullable(conversation.getMessages()).orElse(List.of()).stream()
                .sorted(Comparator
                        .comparing(ChatMessage::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ChatMessage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
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
