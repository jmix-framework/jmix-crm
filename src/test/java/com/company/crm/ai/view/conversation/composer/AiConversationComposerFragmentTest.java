package com.company.crm.ai.view.conversation.composer;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.ai.view.component.support.AiContextRemoveButton;
import com.company.crm.model.client.Client;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.component.gridlayout.GridLayout;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.StandardView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiConversationComposerFragmentTest extends AbstractUiTest {

    @Autowired
    private Fragments fragments;

    @Autowired
    private IdSerialization idSerialization;

    @Test
    void submitsPromptWithStagedContextAndCanBeClearedAfterwards() {
        AiConversationComposerFragment composer = mountedComposer();
        String entityReference = idSerialization.idToString(Id.of(entities.client("Composer Submit Client")));
        PendingAttachmentInput attachment = new PendingAttachmentInput(fileRef("composer-submit.csv"), "composer-submit.csv");
        AtomicReference<AiConversationComposerFragment.Submission> submitted = new AtomicReference<>();

        composer.addEntityReferences(List.of(entityReference));
        composer.addAttachments(List.of(attachment));
        composer.setSubmitHandler(submission -> {
            submitted.set(submission);
            composer.clear();
        });

        ComponentUtil.fireEvent(messageInput(composer),
                new MessageInput.SubmitEvent(messageInput(composer), false, "Analyse staged context"));

        assertThat(submitted.get()).isNotNull();
        assertThat(submitted.get().prompt()).isEqualTo("Analyse staged context");
        assertThat(submitted.get().entityReferences()).containsExactly(entityReference);
        assertThat(submitted.get().attachments()).containsExactly(attachment);
        assertThat(composer.entityReferences()).isEmpty();
        assertThat(composer.attachments()).isEmpty();
        assertThat(pendingContextLayout(composer).isVisible()).isFalse();
    }

    @Test
    void unifiedStagingGridRefreshesWhenItemsAreRemoved() {
        AiConversationComposerFragment composer = mountedComposer();
        String entityReference = idSerialization.idToString(Id.of(entities.client("Composer Grid Client")));
        PendingAttachmentInput attachment = new PendingAttachmentInput(fileRef("composer-grid.csv"), "composer-grid.csv");

        composer.addEntityReferences(List.of(entityReference));
        composer.addAttachments(List.of(attachment));

        VerticalLayout pendingLayout = pendingContextLayout(composer);
        assertThat(pendingLayout.isVisible()).isTrue();
        assertThat(viewTestSupport.descendants(pendingLayout, GridLayout.class)).hasSize(1);
        assertThat(pendingLayout.getChildren().findFirst()).isPresent();

        AiContextRemoveButton firstRemoveButton = viewTestSupport.descendants(pendingLayout, AiContextRemoveButton.class)
                .findFirst()
                .orElseThrow();
        firstRemoveButton.click();

        assertThat(composer.entityReferences().size() + composer.attachments().size()).isEqualTo(1);
        assertThat(viewTestSupport.descendants(pendingLayout, GridLayout.class)).hasSize(1);

        viewTestSupport.descendants(pendingLayout, AiContextRemoveButton.class)
                .findFirst()
                .orElseThrow()
                .click();
        assertThat(composer.isEmpty()).isTrue();
        assertThat(pendingLayout.isVisible()).isFalse();
    }

    @Test
    void variantSelectionAppliesExpectedClassesAndTimelineSizing() {
        AiConversationComposerFragment composer = mountedComposer();

        composer.setVariant(AiConversationComposerFragment.Variant.STARTER);
        assertThat(inputBar(composer).getClassNames()).contains("ai-conversation-starter-input-bar");
        assertThat(pendingContextLayout(composer).getClassNames()).contains("ai-conversation-starter-pending-context");
        assertThat(messageInput(composer).getClassNames()).contains("ai-conversation-starter-message-input");

        composer.setVariant(AiConversationComposerFragment.Variant.TIMELINE);
        assertThat(inputBar(composer).getClassNames()).contains("ai-timeline-input-bar");
        assertThat(pendingContextLayout(composer).getClassNames()).contains("ai-timeline-pending-context");
        assertThat(inputBar(composer).getWidth()).isEqualTo("100%");
        assertThat(pendingContextLayout(composer).getWidth()).isEqualTo("100%");
    }

    private AiConversationComposerFragment mountedComposer() {
        StandardView currentView = UiTestUtils.getCurrentView();
        AiConversationComposerFragment composer = fragments.create(currentView, AiConversationComposerFragment.class);
        currentView.getContent().add(composer);
        return composer;
    }

    private MessageInput messageInput(AiConversationComposerFragment composer) {
        return viewTestSupport.findDescendant(composer, MessageInput.class).orElseThrow();
    }

    private HorizontalLayout inputBar(AiConversationComposerFragment composer) {
        return viewTestSupport.descendants(composer, HorizontalLayout.class)
                .filter(layout -> layout.getClassNames().contains("ai-timeline-input-bar")
                        || layout.getClassNames().contains("ai-conversation-starter-input-bar"))
                .findFirst()
                .orElseThrow();
    }

    private VerticalLayout pendingContextLayout(AiConversationComposerFragment composer) {
        return viewTestSupport.descendants(composer, VerticalLayout.class)
                .filter(layout -> layout.getClassNames().contains("ai-timeline-pending-context")
                        || layout.getClassNames().contains("ai-conversation-starter-pending-context"))
                .findFirst()
                .orElseThrow();
    }

    private static FileRef fileRef(String fileName) {
        return new FileRef("storage", "test/" + fileName, fileName);
    }
}
