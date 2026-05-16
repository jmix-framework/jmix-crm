package com.company.crm.ai.tool;

import com.company.crm.ai.model.AiUiStatusUpdate;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Helper component for publishing AI tool execution status updates to the UI
 * using Spring AI's thread-safe ToolContext mechanism.
 */
@Component
public class AiToolUiStatus {

    public static final String UI_STATUS_UPDATE_CALLBACK = "uiStatusUpdateCallback";

    /**
     * Publishes an in-flight status update message.
     *
     * @param toolContext the Spring AI ToolContext
     * @param message the progress message to show in the UI
     */
    public void update(ToolContext toolContext, String message) {
        publish(toolContext, new AiUiStatusUpdate(message));
    }

    /**
     * Publishes a status update completion message.
     *
     * @param toolContext the Spring AI ToolContext
     * @param baseMessage the base message
     * @param snippet a result snippet to append
     */
    public void complete(ToolContext toolContext, String baseMessage, String snippet) {
        if (snippet == null || snippet.isBlank()) return;
        publish(toolContext, new AiUiStatusUpdate(baseMessage, snippet));
    }

    @SuppressWarnings("unchecked")
    private void publish(ToolContext toolContext, AiUiStatusUpdate update) {
        if (update == null || update.message() == null || update.message().isBlank()) return;

        if (toolContext != null) {
            Object cb = toolContext.getContext().get(UI_STATUS_UPDATE_CALLBACK);
            if (cb instanceof Consumer<?> consumer) {
                Consumer<AiUiStatusUpdate> callback = (Consumer<AiUiStatusUpdate>) consumer;
                callback.accept(update);
            }
        }
    }
}
