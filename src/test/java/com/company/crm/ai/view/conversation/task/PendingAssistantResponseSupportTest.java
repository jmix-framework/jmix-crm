package com.company.crm.ai.view.conversation.task;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiConversation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PendingAssistantResponseSupportTest extends AbstractTest {

    @Autowired
    private PendingAssistantResponseSupport support;

    @Test
    void activePendingResponseLocksSubmissionsUntilResolvedOrCanceled() {
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> conv.setTitle("Lockout"));
        List<Boolean> inputStates = new ArrayList<>();

        support.registerPendingResponse(conversation, inputStates::add);

        assertThat(support.isPending(conversation)).isTrue();
        assertThat(support.canSubmit(conversation)).isFalse();
        assertThat(inputStates).containsExactly(false);

        support.resolvePendingResponse(conversation);

        assertThat(support.isPending(conversation)).isFalse();
        assertThat(support.canSubmit(conversation)).isTrue();
        assertThat(inputStates).containsExactly(false, true);

        support.registerPendingResponse(conversation, inputStates::add);
        support.cancelPendingResponse(conversation);

        assertThat(support.canSubmit(conversation)).isTrue();
        assertThat(inputStates).containsExactly(false, true, false, true);
    }

    @Test
    void pendingResponseSynchronizesInputStateAcrossMultipleViews() {
        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> conv.setTitle("Multi-view sync"));
        List<Boolean> firstViewStates = new ArrayList<>();
        List<Boolean> secondViewStates = new ArrayList<>();

        support.registerPendingResponse(conversation, firstViewStates::add);
        support.registerPendingResponse(conversation, secondViewStates::add);

        assertThat(firstViewStates).containsExactly(false);
        assertThat(secondViewStates).containsExactly(false);

        support.resolvePendingResponse(conversation);

        assertThat(firstViewStates).containsExactly(false, true);
        assertThat(secondViewStates).containsExactly(false, true);
    }
}
