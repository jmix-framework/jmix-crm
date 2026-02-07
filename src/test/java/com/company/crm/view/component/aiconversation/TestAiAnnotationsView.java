package com.company.crm.view.component.aiconversation;

// Config import removed - simplified demo
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test view to demonstrate all Jmix framework annotation patterns (@Install, @Supply, @Subscribe)
 * with the AiConversationComponent.
 */
@Route("test-ai-annotations")
@ViewController("TestAiAnnotations")
@ViewDescriptor("test-ai-annotations-view.xml")
public class TestAiAnnotationsView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(TestAiAnnotationsView.class);

    @ViewComponent
    public AiConversationComponent aiComponent;

    /**
     * @Install for messageProcessor - demonstrates function-based @Install pattern.
     * The framework automatically calls this method when a user sends a message.
     */
    @Install(to = "aiComponent", subject = "messageProcessor")
    private String processMessage(String message) {
        log.info("@Install messageProcessor called with: {}", message);
        return "Annotation processed: " + message.toUpperCase() + " (processed via @Install)";
    }

    /**
     * Manual registration of messageSentHandler - demonstrates programmatic handler registration.
     * This shows how to add handlers without @Install annotation.
     */
    @Subscribe
    public void onReady(ReadyEvent event) {
        // Manually register the message sent handler
        aiComponent.addMessageSentHandler(this::onMessageSent);
    }

    private void onMessageSent(String message) {
        log.info("Manual messageSentHandler called: {}", message);
        // This could trigger additional business logic like logging, analytics, etc.
    }

    /**
     * @Supply for configuration - demonstrates supplier-based @Supply pattern.
     * The framework automatically calls this method to get the component configuration.
     * Currently commented out pending proper method naming resolution.
     */
    // @Supply(to = "aiComponent", subject = "configuration")
    // private AiConversationConfig getConfiguration() {
    //     log.info("@Supply configuration called - providing custom configuration");
    //     return AiConversationConfig.builder()
    //         .showTypingIndicator(false)
    //         .maxHistoryItems(25)
    //         .processingTimeout(Duration.ofSeconds(10))
    //         .enableMarkdown(false)
    //         .placeholderText("Type here to test annotations...")
    //         .build();
    // }

    /**
     * @Subscribe for events - demonstrates event-based @Subscribe pattern.
     * The framework automatically calls this method when MessageSentEvent is fired.
     * Currently commented out as it requires additional Jmix framework integration.
     */
    // @Subscribe(id = "aiComponent", subject = "messageSentEvent")
    // private void onMessageSentEvent(AiConversationComponent.MessageSentEvent event) {
    //     log.info("@Subscribe messageSentEvent called: message={}, conversationId={}",
    //         event.getMessage(), event.getConversationId());
    //     // This could trigger additional event-based logic
    // }
}