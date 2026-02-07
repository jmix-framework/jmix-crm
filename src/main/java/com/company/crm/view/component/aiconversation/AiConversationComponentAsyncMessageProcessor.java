package com.company.crm.view.component.aiconversation;

import com.company.crm.ai.service.AiConversationService;
import com.vaadin.flow.component.UI;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

/**
 * Generic utility for handling asynchronous conversation processing with UI updates.
 * Provides the common pattern of async processing with UI thread safety for AiConversationComponent.
 */
@Component
public class AiConversationComponentAsyncMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(AiConversationComponentAsyncMessageProcessor.class);

    private final UiAsyncTasks uiAsyncTasks;
    private final AiConversationService aiConversationService;

    public AiConversationComponentAsyncMessageProcessor(UiAsyncTasks uiAsyncTasks,
                                                      AiConversationService aiConversationService) {
        this.uiAsyncTasks = uiAsyncTasks;
        this.aiConversationService = aiConversationService;
    }

    /**
     * Processes a user message asynchronously using the provided processor and updates the AI component.
     *
     * @param userMessage    the user's message
     * @param conversationId the conversation ID
     * @param aiComponent    the AI component to update
     * @param processor      function that processes the message (e.g., AI service call)
     */
    public void processMessage(String userMessage, String conversationId,
                               AiConversationComponent aiComponent,
                               Function<ConversationRequest, String> processor) {
        log.info("Processing message async: {}", userMessage);

        ConversationRequest request = new ConversationRequest(userMessage, conversationId);

        uiAsyncTasks.supplierConfigurer(() -> {
                    try {
                        String response = processor.apply(request);
                        log.info("Response generated: {}", response);
                        return response;
                    } catch (Exception e) {
                        log.error("Error processing message async", e);
                        return "I'm sorry, I encountered an error while processing your request: " + e.getMessage() +
                                "\n\nPlease try rephrasing your question or contact support if the problem persists.";
                    }
                })
                .withResultHandler(response -> {
                    log.info("ResultHandler called with response: {}", response != null ? "Response received" : "NULL response");

                    // Check if UI is available
                    UI currentUI = null;
                    try {
                        currentUI = UI.getCurrent();
                        log.info("UI.getCurrent() returned: {}", currentUI != null ? "Valid UI" : "NULL UI");
                    } catch (Exception e) {
                        log.error("Exception getting UI.getCurrent(): {}", e.getMessage(), e);
                    }

                    if (currentUI != null) {
                        log.info("Attempting UI.access() call...");
                        currentUI.access(() -> {
                            log.info("Inside UI.access() - about to add message to component");
                            try {
                                aiComponent.addMessage(response, "Assistant", 2);
                                log.info("addMessage() completed successfully");

                                aiComponent.hideProgress();
                                log.info("hideProgress() completed");

                                aiComponent.getMessageInput().setEnabled(true);
                                log.info("messageInput.setEnabled(true) completed");

                                log.info("UI update sequence completed successfully");
                            } catch (Exception e) {
                                log.error("Exception during UI update inside access(): {}", e.getMessage(), e);
                            }
                        });
                        log.info("UI.access() call completed");
                    } else {
                        log.error("Cannot update UI - UI.getCurrent() is null");
                    }
                })
                .withExceptionHandler(e -> {
                    log.error("Async processing failed", e);
                    UI.getCurrent().access(() -> {
                        log.error("Adding error message to UI");
                        aiComponent.addMessage("I'm sorry, something went wrong while processing your request.", "Assistant", 2);
                        aiComponent.hideProgress();
                        aiComponent.getMessageInput().setEnabled(true);
                    });
                })
                .supplyAsync();
    }
}