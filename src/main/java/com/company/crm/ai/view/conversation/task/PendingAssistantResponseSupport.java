package com.company.crm.ai.view.conversation.task;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class PendingAssistantResponseSupport {

    private final DataManager dataManager;
    private final Map<UUID, PendingResponseState> pendingResponses = new ConcurrentHashMap<>();

    public PendingAssistantResponseSupport(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public PendingResponseRegistration registerPendingResponse(AiConversation conversation,
                                                               Consumer<Boolean> inputEnabledListener) {
        if (conversation == null || conversation.getId() == null) {
            return PendingResponseRegistration.NOOP;
        }

        PendingResponseState state = pendingResponses.computeIfAbsent(conversation.getId(), ignored -> new PendingResponseState());
        state.addListener(inputEnabledListener);
        if (inputEnabledListener != null) {
            inputEnabledListener.accept(false);
        }

        return () -> {
            state.removeListener(inputEnabledListener);
            if (!state.hasListeners()) {
                pendingResponses.remove(conversation.getId(), state);
            }
        };
    }

    public boolean canSubmit(AiConversation conversation) {
        return !isPending(conversation);
    }

    public boolean isPending(AiConversation conversation) {
        return conversation != null
                && conversation.getId() != null
                && pendingResponses.containsKey(conversation.getId());
    }

    public void resolvePendingResponse(AiConversation conversation) {
        completePendingResponse(conversation);
    }

    public void cancelPendingResponse(AiConversation conversation) {
        completePendingResponse(conversation);
    }

    private void completePendingResponse(AiConversation conversation) {
        if (conversation == null || conversation.getId() == null) {
            return;
        }

        PendingResponseState state = pendingResponses.remove(conversation.getId());
        if (state != null) {
            state.notifyInputEnabled(true);
        }
    }

    public Optional<ChatMessage> findPendingUserMessage(AiConversation conversation) {
        if (conversation == null) {
            return Optional.empty();
        }

        ChatMessage lastMessage = loadLatestPersistedMessage(conversation)
                .or(() -> latestLoadedMessage(conversation))
                .orElse(null);

        return lastMessage != null && ChatMessageType.USER.equals(lastMessage.getType())
                ? Optional.of(lastMessage)
                : Optional.empty();
    }

    private Optional<ChatMessage> loadLatestPersistedMessage(AiConversation conversation) {
        if (conversation.getId() == null) {
            return Optional.empty();
        }

        return dataManager.load(ChatMessage.class)
                .query("e.conversation.id = :conversationId order by e.createdDate desc, e.id desc")
                .parameter("conversationId", conversation.getId())
                .maxResults(1)
                .optional();
    }

    private Optional<ChatMessage> latestLoadedMessage(AiConversation conversation) {
        List<ChatMessage> messages = conversation.getMessages();
        if (messages == null || messages.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(messages.stream()
                .filter(message -> message.getCreatedDate() != null)
                .max(Comparator.comparing(ChatMessage::getCreatedDate))
                .orElse(messages.get(messages.size() - 1)));
    }

    public interface PendingResponseRegistration extends AutoCloseable {

        PendingResponseRegistration NOOP = () -> {
        };

        @Override
        void close();
    }

    private static class PendingResponseState {
        private final List<Consumer<Boolean>> inputEnabledListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

        void addListener(Consumer<Boolean> listener) {
            if (listener != null) {
                inputEnabledListeners.add(listener);
            }
        }

        void removeListener(Consumer<Boolean> listener) {
            inputEnabledListeners.remove(listener);
        }

        boolean hasListeners() {
            return !inputEnabledListeners.isEmpty();
        }

        void notifyInputEnabled(boolean enabled) {
            inputEnabledListeners.forEach(listener -> listener.accept(enabled));
        }
    }
}
