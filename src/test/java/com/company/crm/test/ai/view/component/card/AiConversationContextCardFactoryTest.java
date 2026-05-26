package com.company.crm.test.ai.view.component.card;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.company.crm.ai.view.component.card.AiConversationContextCardFactory;
import com.company.crm.ai.view.component.card.AiPendingEntityReferenceCard;
import com.company.crm.ai.view.component.card.AiAttachmentCard;
import com.vaadin.flow.component.Component;
import io.jmix.core.FileRef;
import io.jmix.core.Metadata;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.gridlayout.GridLayout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiConversationContextCardFactoryTest extends AbstractUiTest {

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Metadata metadata;

    @Test
    void createsUnifiedPendingContextCardsGridForEntityReferencesAndAttachments() {
        AiConversationContextCardFactory factory = new AiConversationContextCardFactory(
                uiComponents,
                metadata,
                ignored -> {
                },
                ignored -> {
                }
        );
        List<String> entityReferences = List.of("crm_Client-placeholder");
        List<PendingAttachmentInput> attachments = List.of(
                new PendingAttachmentInput(fileRef("pending-one.csv"), "pending-one.csv"),
                new PendingAttachmentInput(fileRef("pending-two.txt"), "pending-two.txt")
        );

        Component component = factory.createPendingContextCardsGrid(
                entityReferences,
                ignored -> {
                },
                attachments,
                ignored -> {
                }
        );

        assertThat(component).isInstanceOf(GridLayout.class);
        assertThat(component.getClassNames()).contains("ai-timeline-context-cards");
        assertThat(component.getChildren()).hasSize(entityReferences.size() + attachments.size());
        assertThat(viewTestSupport.descendants(component, AiPendingEntityReferenceCard.class)).hasSize(1);
        assertThat(viewTestSupport.descendants(component, AiAttachmentCard.class)).hasSize(2);
    }

    private static FileRef fileRef(String fileName) {
        return new FileRef("storage", "test/" + fileName, fileName);
    }
}
