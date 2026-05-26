package com.company.crm.ai.view.timeline;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.view.component.card.AiAttachmentCard;
import com.company.crm.ai.view.component.card.AiConversationContextCardFactory;
import com.company.crm.ai.view.component.card.AiEntityReferenceCard;
import com.company.crm.model.client.Client;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.markdown.Markdown;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.gridlayout.GridLayout;
import io.jmix.flowui.view.MessageBundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiTimelineMessageRowTest extends AbstractUiTest {

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Metadata metadata;

    @Autowired
    private IdSerialization idSerialization;

    @Autowired
    private Messages messages;

    @Autowired
    private ObjectProvider<MessageBundle> messageBundles;

    @Test
    void rendersAssistantMarkdownWithTablesLinksAndTimelineClass() {
        String markdown = """
                Here are the invoices:

                | Invoice | Client | Total |
                | --- | --- | ---: |
                | INV-101 | Thompson LLC | 1200 |

                See [invoice details](https://example.test/invoices/INV-101).
                """;
        ChatMessage message = message(ChatMessageType.ASSISTANT, markdown);

        Component component = timelineFactory().createTimelineComponent(TimelineItem.assistant(message));

        AiTimelineMessageRow row = assertMessageRow(component);
        Markdown markdownComponent = viewTestSupport.findDescendant(row, Markdown.class).orElseThrow();
        assertThat(markdownComponent.getClassNames()).contains("ai-timeline-markdown");
        assertThat(markdownComponent.getContent())
                .contains("| Invoice | Client | Total |")
                .contains("| INV-101 | Thompson LLC | 1200 |")
                .contains("[invoice details](https://example.test/invoices/INV-101)");
    }

    @Test
    void rendersSingleCombinedContextGridForEntityReferencesAndAttachments() {
        Client client = entities.client("Timeline Context Client");
        String entityReference = idSerialization.idToString(Id.of(client));

        AiConversation conversation = createAndSaveEntity(AiConversation.class, conv -> conv.setTitle("Timeline context"));
        ChatMessage message = createAndSaveEntity(ChatMessage.class, msg -> {
            msg.setConversation(conversation);
            msg.setType(ChatMessageType.USER);
            msg.setContent("Please use this context");
        });
        ChatMessageEntityReference savedReference = createAndSaveEntity(ChatMessageEntityReference.class, ref -> {
            ref.setMessage(message);
            ref.setEntityReference(entityReference);
            ref.setSortOrder(0);
        });
        AiConversationAttachment savedAttachment = createAndSaveEntity(AiConversationAttachment.class, attachment -> {
            attachment.setMessage(message);
            attachment.setFile(new FileRef("storage", "test/timeline-context.csv", "timeline-context.csv"));
            attachment.setFileName("timeline-context.csv");
            attachment.setTitle("timeline-context.csv");
            attachment.setOrigin(AiAttachmentOrigin.USER_UPLOADED);
        });

        message.setEntityReferences(List.of(savedReference));
        message.setAttachments(List.of(savedAttachment));

        Component component = timelineFactory().createTimelineComponent(TimelineItem.user(message));

        AiTimelineMessageRow row = assertMessageRow(component);
        assertThat(row.getClassNames()).contains("ai-timeline-message-row-context");
        assertThat(viewTestSupport.descendants(row, GridLayout.class)).hasSize(1);
        assertThat(viewTestSupport.descendants(row, AiEntityReferenceCard.class)).hasSize(1);
        assertThat(viewTestSupport.descendants(row, AiAttachmentCard.class)).hasSize(1);
        assertThat(viewTestSupport.textsByClassName(row, "ai-timeline-attachment-title"))
                .contains("Timeline Context Client", "timeline-context.csv");
        assertThat(viewTestSupport.textsByClassName(row, "ai-timeline-attachment-meta"))
                .contains(messages.getMessage("com.company.crm.model.client/Client"))
                .contains(messages.getMessage("com.company.crm.ai.view.conversation/attachmentsSourceUser"));
    }

    private AiTimelineMessageRow assertMessageRow(Component component) {
        assertThat(component).isInstanceOf(AiTimelineMessageRow.class);
        return (AiTimelineMessageRow) component;
    }

    private AiTimelineComponentFactory timelineFactory() {
        return new AiTimelineComponentFactory(
                uiComponents,
                conversationMessageBundle(),
                new AiConversationContextCardFactory(uiComponents, metadata, ignored -> {
                }, ignored -> {
                }),
                ignored -> "Current user",
                ignored -> "09:30",
                () -> null
        );
    }

    private MessageBundle conversationMessageBundle() {
        MessageBundle messageBundle = messageBundles.getObject();
        messageBundle.setMessageGroup("com.company.crm.ai.view.conversation");
        return messageBundle;
    }

    private static ChatMessage message(ChatMessageType type, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setType(type);
        message.setContent(content);
        message.setCreatedDate(OffsetDateTime.now());
        return message;
    }
}
