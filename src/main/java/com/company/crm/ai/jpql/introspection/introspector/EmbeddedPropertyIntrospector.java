package com.company.crm.ai.jpql.introspection.introspector;

import com.company.crm.ai.jpql.introspection.model.AiPropertyDescriptor;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.model.MetaProperty;
import org.springframework.stereotype.Component;

/**
 * Introspects EMBEDDED MetaProperties into AiProperty objects.
 */
@Component
public class EmbeddedPropertyIntrospector implements MetaPropertyIntrospector {

    private final MetadataTools metadataTools;
    private final MessageTools messageTools;

    public EmbeddedPropertyIntrospector(MetadataTools metadataTools, MessageTools messageTools) {
        this.metadataTools = metadataTools;
        this.messageTools = messageTools;
    }

    @Override
    public boolean supports(MetaProperty property) {
        return property.getType() == MetaProperty.Type.EMBEDDED;
    }

    @Override
    public AiPropertyDescriptor introspect(MetaProperty property) {
        if (!supports(property)) {
            return null;
        }

        return new AiPropertyDescriptor(
                getPropertyCaption(property, messageTools),
                metadataTools.getMetaAnnotationValue(property, Comment.class),
                property.getType().name().toLowerCase(),
                property.getRange().asClass().getName(),
                null,
                true,
                null,
                null,
                null,
                null
        );
    }
}
