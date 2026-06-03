package com.company.crm.test.ai.view.component.card;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.service.AiEntityReferenceResolver;
import com.company.crm.ai.view.component.card.AiPendingEntityReferenceCard;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientType;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiPendingEntityReferenceCardTest extends AbstractUiTest {

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private IdSerialization idSerialization;

    @Autowired
    private Messages messages;

    @Test
    void rendersRealClientEntityReference() {
        Client client = createAndSaveEntity(Client.class, entity -> {
            entity.setName("Thompson LLC");
            entity.setType(ClientType.BUSINESS);
            entity.setAddress(entities.address());
        });
        String entityReference = idSerialization.idToString(Id.of(client));

        AiPendingEntityReferenceCard card = uiComponents.create(AiPendingEntityReferenceCard.class);
        card.setPendingEntityReference(entityReference, ignored -> {
        }, ignored -> {
        });

        assertThat(viewTestSupport.textByClassName(card, "ai-timeline-attachment-title"))
                .contains("Thompson LLC");
        assertThat(viewTestSupport.textByClassName(card, "ai-timeline-attachment-meta"))
                .contains(messages.getMessage("com.company.crm.model.client/Client"));
    }

    @Test
    void fallsBackWhenEntityReferenceCannotBeResolved() {
        String missingClientReference = idSerialization.idToString(Id.of(UUID.randomUUID(), Client.class));

        AiPendingEntityReferenceCard card = uiComponents.create(AiPendingEntityReferenceCard.class);
        card.setPendingEntityReference(missingClientReference, ignored -> {
        }, ignored -> {
        });

        assertThat(viewTestSupport.textByClassName(card, "ai-timeline-attachment-title"))
                .contains(messages.getMessage(AiEntityReferenceResolver.class, "entityReferenceFallbackTitle"));
        assertThat(viewTestSupport.textByClassName(card, "ai-timeline-attachment-meta"))
                .contains(messages.getMessage(AiEntityReferenceResolver.class, "entityReferenceUnavailable"));
    }
}
