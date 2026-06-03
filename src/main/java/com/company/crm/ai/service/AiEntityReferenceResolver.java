package com.company.crm.ai.service;

import com.company.crm.ai.context.AiContextEntityDefinition;
import com.company.crm.ai.context.AiContextEntityRegistry;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.jmix.core.*;
import io.jmix.core.metamodel.model.MetaClass;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolver that parses serialized Jmix entity references (e.g. from entity IDs/links)
 * and resolves them into rich {@link EntityReferenceViewData} for rendering.
 * Provides a robust fallback representation in case the reference is empty,
 * invalid, or the underlying entity is not found.
 */
@Component
public class AiEntityReferenceResolver {

    private final Messages messages;
    private final IdSerialization idSerialization;
    private final DataManager dataManager;
    private final MetadataTools metadataTools;
    private final AiContextEntityRegistry contextEntityRegistry;
    private final Metadata metadata;
    private final MessageTools messageTools;

    public AiEntityReferenceResolver(Messages messages,
                                     IdSerialization idSerialization,
                                     DataManager dataManager,
                                     MetadataTools metadataTools,
                                     AiContextEntityRegistry contextEntityRegistry,
                                     Metadata metadata,
                                     MessageTools messageTools) {
        this.messages = messages;
        this.idSerialization = idSerialization;
        this.dataManager = dataManager;
        this.metadataTools = metadataTools;
        this.contextEntityRegistry = contextEntityRegistry;
        this.metadata = metadata;
        this.messageTools = messageTools;
    }

    public EntityReferenceViewData resolve(String entityReference) {
        if (entityReference == null || entityReference.isBlank()) {
            return fallback();
        }

        try {
            Id<Object> id = idSerialization.stringToId(entityReference);
            Object entity = dataManager.load(id).one();
            String title = Optional.of(metadataTools.getInstanceName(entity))
                    .filter(name -> !name.isBlank())
                    .orElseGet(() -> id.getEntityClass().getSimpleName());

            VaadinIcon icon = contextEntityRegistry.findDefinition(id.getEntityClass())
                    .map(AiContextEntityDefinition::icon)
                    .orElse(VaadinIcon.DATABASE);
            MetaClass metaClass = metadata.getClass(id.getEntityClass());
            String caption = messageTools.getEntityCaption(metaClass);

            return new EntityReferenceViewData(title, caption, icon);
        } catch (Exception e) {
            return fallback();
        }
    }

    private EntityReferenceViewData fallback() {
        return new EntityReferenceViewData(
                messages.getMessage(getClass(), "entityReferenceFallbackTitle"),
                messages.getMessage(getClass(), "entityReferenceUnavailable"),
                VaadinIcon.DATABASE
        );
    }
}
