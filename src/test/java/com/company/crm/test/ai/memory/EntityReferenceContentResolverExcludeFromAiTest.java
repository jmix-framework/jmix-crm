package com.company.crm.test.ai.memory;

import com.company.crm.AbstractTest;
import com.company.crm.ai.memory.EntityReferenceContentResolver;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.Metadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A client added to the chat context carries its contacts to the model, but not the attributes marked
 * {@link io.jmix.aitools.ExcludeFromAi} (Contact.phone, Contact.email).
 */
class EntityReferenceContentResolverExcludeFromAiTest extends AbstractTest {

    @Autowired
    private EntityReferenceContentResolver resolver;
    @Autowired
    private IdSerialization idSerialization;
    @Autowired
    private Metadata metadata;

    @Test
    void clientContextLeavesOutContactPhoneAndEmail() {
        Client client = entities.client("Context Client");
        Contact contact = entities.contact(client, "Jane Context", "Buyer");
        contact.setPhone("+7 900 111-22-33");
        contact.setEmail("jane.context@example.com");
        saveWithoutReload(contact);

        ChatMessageEntityReference reference = metadata.create(ChatMessageEntityReference.class);
        reference.setEntityReference(idSerialization.idToString(Id.of(client)));

        String context = systemAuthenticator.withSystem(() -> resolver.resolveContext(List.of(reference)));

        assertThat(context).contains("Context Client", "Jane Context", "Buyer")
                .doesNotContain("+7 900 111-22-33", "jane.context@example.com");
    }
}
