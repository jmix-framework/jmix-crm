package com.company.crm.ai.view.timeline;

import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class TimelineItem {

    private final TimelineItemKind kind;
    private final ChatMessage message;
    private final List<AiUiStatusUpdate> statusUpdates;

    private TimelineItem(TimelineItemKind kind, ChatMessage message, List<AiUiStatusUpdate> statusUpdates) {
        this.kind = kind;
        this.message = message;
        this.statusUpdates = statusUpdates;
    }

    public static TimelineItem user(ChatMessage message) {
        return new TimelineItem(TimelineItemKind.USER, message, List.of());
    }

    public static TimelineItem assistant(ChatMessage message) {
        return new TimelineItem(TimelineItemKind.ASSISTANT, message, List.of());
    }

    public static TimelineItem thinking(ChatMessage placeholder) {
        return new TimelineItem(TimelineItemKind.ASSISTANT_THINKING, placeholder, new ArrayList<>());
    }

    public TimelineItemKind kind() {
        return kind;
    }

    public ChatMessage message() {
        return message;
    }

    public List<AiUiStatusUpdate> statusUpdates() {
        return statusUpdates;
    }

}
