package com.company.crm.ai.tool;

import com.company.crm.ai.model.AiUiStatusUpdate;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class AiToolStatusPublisher {

    public static final String STATUS_UPDATE_CALLBACK = "aiToolStatusUpdateCallback";

    public void update(ToolContext toolContext, String message) {
        publish(toolContext, new AiUiStatusUpdate(message));
    }

    public void complete(ToolContext toolContext, String baseMessage, String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return;
        }
        publish(toolContext, new AiUiStatusUpdate(baseMessage, snippet));
    }

    @SuppressWarnings("unchecked")
    private void publish(ToolContext toolContext, AiUiStatusUpdate update) {
        if (update == null || update.message() == null || update.message().isBlank() || toolContext == null) {
            return;
        }

        Object callback = toolContext.getContext().get(STATUS_UPDATE_CALLBACK);
        if (callback instanceof Consumer<?> consumer) {
            ((Consumer<AiUiStatusUpdate>) consumer).accept(update);
        }
    }
}
