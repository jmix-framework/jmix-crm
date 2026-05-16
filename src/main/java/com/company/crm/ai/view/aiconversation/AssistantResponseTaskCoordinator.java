package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.CrmAnalyticsService;
import io.jmix.core.DataManager;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.BackgroundWorker;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import io.jmix.flowui.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
class AssistantResponseTaskCoordinator {

    private static final Logger log = LoggerFactory.getLogger(AssistantResponseTaskCoordinator.class);

    private final CrmAnalyticsService crmAnalyticsService;
    private final BackgroundWorker backgroundWorker;
    private final DataManager dataManager;

    AssistantResponseTaskCoordinator(CrmAnalyticsService crmAnalyticsService,
                                     BackgroundWorker backgroundWorker,
                                     DataManager dataManager) {
        this.crmAnalyticsService = crmAnalyticsService;
        this.backgroundWorker = backgroundWorker;
        this.dataManager = dataManager;
    }

    void run(View<?> owner,
             AiConversation conversation,
             ChatMessage savedUserMessage,
             Consumer<AiUiStatusUpdate> progressHandler,
             Consumer<ChatMessage> doneHandler,
             Runnable failureHandler) {
        BackgroundTask<AiUiStatusUpdate, String> task = new BackgroundTask<>(5, TimeUnit.MINUTES, owner) {
            @Override
            public String run(TaskLifeCycle<AiUiStatusUpdate> taskLifeCycle) {
                return crmAnalyticsService.processUserMessage(
                        savedUserMessage.getId(),
                        statusUpdate -> publishUiStatusUpdate(taskLifeCycle, statusUpdate)
                );
            }

            @Override
            public void progress(List<AiUiStatusUpdate> changes) {
                changes.forEach(progressHandler);
            }

            @Override
            public void done(String response) {
                doneHandler.accept(loadLatestAssistantMessage(conversation));
            }

            @Override
            public boolean handleException(Exception ex) {
                log.error("Error processing message async", ex);
                failureHandler.run();
                return true;
            }

            @Override
            public boolean handleTimeoutException() {
                log.error("Timed out while processing AI message {}", savedUserMessage.getId());
                failureHandler.run();
                return true;
            }
        };

        backgroundWorker.handle(task).execute();
    }

    private void publishUiStatusUpdate(TaskLifeCycle<AiUiStatusUpdate> taskLifeCycle,
                                       AiUiStatusUpdate statusUpdate) {
        if (statusUpdate == null || taskLifeCycle.isInterrupted()) {
            return;
        }

        try {
            taskLifeCycle.publish(statusUpdate);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI UI status update publishing was interrupted", e);
        }
    }

    private ChatMessage loadLatestAssistantMessage(AiConversation conversation) {
        if (conversation == null || conversation.getId() == null) {
            return null;
        }
        return dataManager.load(ChatMessage.class)
                .query("e.conversation.id = :convId and e.type = :type order by e.createdDate desc, e.id desc")
                .parameter("convId", conversation.getId())
                .parameter("type", ChatMessageType.ASSISTANT.getId())
                .maxResults(1)
                .optional()
                .orElse(null);
    }
}
