package com.company.crm.ai.view.conversation.task;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiUiStatusUpdate;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.service.CrmAnalyticsService;
import io.jmix.core.DataManager;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.BackgroundTaskHandler;
import io.jmix.flowui.backgroundtask.BackgroundWorker;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import io.jmix.flowui.backgroundtask.UIAccessor;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

class AssistantResponseTaskCoordinatorTest extends AbstractUiTest {

    @Autowired
    private DataManager dataManager;

    @MockitoBean
    private CrmAnalyticsService crmAnalyticsService;

    private AssistantResponseTaskCoordinator coordinator;

    @BeforeEach
    void setUpCoordinator() {
        coordinator = new AssistantResponseTaskCoordinator(
                crmAnalyticsService,
                new ImmediateBackgroundWorker(),
                dataManager
        );
    }

    @Test
    void backgroundTaskPublishesProgressAndLoadsFinalAssistantMessage() {
        AiConversation conversation = conversation("Coordinator success");
        ChatMessage userMessage = userMessage(conversation, "Run cashflow analysis");
        ChatMessage assistantMessage = assistantMessage(conversation, "Cashflow looks healthy.");
        List<AiUiStatusUpdate> progress = new ArrayList<>();
        AtomicReference<ChatMessage> doneMessage = new AtomicReference<>();

        doAnswer((Answer<String>) invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<AiUiStatusUpdate> callback = invocation.getArgument(1, Consumer.class);
            callback.accept(new AiUiStatusUpdate("Querying invoices"));
            callback.accept(new AiUiStatusUpdate("Checking payments", "2 payments"));
            return assistantMessage.getContent();
        }).when(crmAnalyticsService).processUserMessage(eq(userMessage.getId()), any());

        coordinator.run(
                UiTestUtils.getCurrentView(),
                conversation,
                userMessage,
                progress::add,
                doneMessage::set,
                () -> {
                }
        );

        assertThat(progress).extracting(AiUiStatusUpdate::message)
                .contains("Querying invoices", "Checking payments");
        assertThat(doneMessage.get()).isNotNull();
        assertThat(doneMessage.get().getId()).isEqualTo(assistantMessage.getId());
    }

    @Test
    void backgroundTaskFailureInvokesRecoveryHandler() {
        AiConversation conversation = conversation("Coordinator failure");
        ChatMessage userMessage = userMessage(conversation, "Trigger failure");
        AtomicReference<Boolean> failure = new AtomicReference<>(false);

        doAnswer((Answer<String>) invocation -> {
            throw new IllegalStateException("LLM unavailable");
        }).when(crmAnalyticsService).processUserMessage(eq(userMessage.getId()), any());

        coordinator.run(
                UiTestUtils.getCurrentView(),
                conversation,
                userMessage,
                ignored -> {
                },
                ignored -> {
                },
                () -> failure.set(true)
        );

        assertThat(failure.get()).isTrue();
    }

    private AiConversation conversation(String title) {
        return createAndSaveEntity(AiConversation.class, conv -> conv.setTitle(title + " " + UUID.randomUUID()));
    }

    private ChatMessage userMessage(AiConversation conversation, String content) {
        return createAndSaveEntity(ChatMessage.class, message -> {
            message.setConversation(conversation);
            message.setType(ChatMessageType.USER);
            message.setContent(content);
            message.setCreatedDate(OffsetDateTime.now());
        });
    }

    private ChatMessage assistantMessage(AiConversation conversation, String content) {
        return createAndSaveEntity(ChatMessage.class, message -> {
            message.setConversation(conversation);
            message.setType(ChatMessageType.ASSISTANT);
            message.setContent(content);
            message.setCreatedDate(OffsetDateTime.now().plusSeconds(1));
        });
    }

    private static class ImmediateBackgroundWorker implements BackgroundWorker {

        @Override
        public <T, V> BackgroundTaskHandler<V> handle(BackgroundTask<T, V> task) {
            return new ImmediateBackgroundTaskHandler<>(task);
        }

        @Override
        public UIAccessor getUIAccessor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkUIAccess() {
        }
    }

    private static class ImmediateBackgroundTaskHandler<T, V> implements BackgroundTaskHandler<V> {

        private final BackgroundTask<T, V> task;
        private V result;
        private boolean done;
        private boolean cancelled;

        private ImmediateBackgroundTaskHandler(BackgroundTask<T, V> task) {
            this.task = task;
        }

        @Override
        public void execute() {
            try {
                result = task.run(new ImmediateTaskLifeCycle<>(task));
                task.done(result);
            } catch (Exception ex) {
                task.handleException(ex);
            } finally {
                done = true;
            }
        }

        @Override
        public boolean cancel() {
            cancelled = true;
            return true;
        }

        @Override
        public V getResult() {
            return result;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isAlive() {
            return !done && !cancelled;
        }
    }

    private static class ImmediateTaskLifeCycle<T, V> implements TaskLifeCycle<T> {

        private final BackgroundTask<T, V> task;

        private ImmediateTaskLifeCycle(BackgroundTask<T, V> task) {
            this.task = task;
        }

        @SafeVarargs
        @Override
        public final void publish(T... changes) {
            task.progress(List.of(changes));
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public Map<String, Object> getParams() {
            return Collections.emptyMap();
        }
    }
}
