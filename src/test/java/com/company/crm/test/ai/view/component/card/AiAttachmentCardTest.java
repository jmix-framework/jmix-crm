package com.company.crm.test.ai.view.component.card;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.view.component.card.AiAttachmentCard;
import com.company.crm.ai.view.component.support.AiContextRemoveButton;
import com.vaadin.flow.component.icon.Icon;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AiAttachmentCardTest extends AbstractUiTest {

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Messages messages;

    @Test
    void rendersAttachmentInfoWithLocalizedMetaAndMediaIcon() {
        AiConversationAttachment attachment = new AiConversationAttachment();
        attachment.setFileName("customer-notes.pdf");
        attachment.setOrigin(AiAttachmentOrigin.USER_UPLOADED);

        AiAttachmentCard card = uiComponents.create(AiAttachmentCard.class);
        card.setAttachment(attachment, ignored -> {
        });

        assertThat(viewTestSupport.textByClassName(card, "ai-timeline-attachment-title"))
                .contains("customer-notes.pdf");
        assertThat(viewTestSupport.textByClassName(card, "ai-timeline-attachment-meta"))
                .contains(messages.getMessage("com.company.crm.ai.view.conversation/attachmentsSourceUser"));
        assertThat(viewTestSupport.descendants(card, Icon.class)
                .filter(icon -> icon.getClassNames().contains("ai-timeline-attachment-icon"))
                .findFirst())
                .isPresent()
                .get()
                .extracting(icon -> icon.getElement().getAttribute("icon"))
                .isEqualTo("vaadin:file-text-o");
    }

    @Test
    void rendersPendingRemoveButtonAndInvokesCallback() {
        AiConversationAttachment attachment = new AiConversationAttachment();
        attachment.setFileName("pending-context.csv");
        attachment.setOrigin(AiAttachmentOrigin.USER_UPLOADED);
        AtomicBoolean removed = new AtomicBoolean(false);

        AiAttachmentCard card = uiComponents.create(AiAttachmentCard.class);
        card.setAttachment(attachment, ignored -> {
        }, () -> removed.set(true));

        assertThat(card.getClassNames()).contains("ai-timeline-pending-card");

        AiContextRemoveButton removeButton = viewTestSupport.findDescendant(card, AiContextRemoveButton.class)
                .orElseThrow();
        removeButton.click();

        assertThat(removed).isTrue();
    }
}
