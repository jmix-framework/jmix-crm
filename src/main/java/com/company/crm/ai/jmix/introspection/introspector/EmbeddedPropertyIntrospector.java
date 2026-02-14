package com.company.crm.ai.jmix.introspection.introspector;

import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
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

        String javaType = property.getRange().asClass().getName();
        String type = property.getType().name().toLowerCase();
        Boolean embedded = true;
        String comment = metadataTools.getMetaAnnotationValue(property, Comment.class);
        String caption = getPropertyCaption(property);

        return new AiPropertyDescriptor(caption, comment, type, javaType, null, embedded, null, null,
                null, null, null);
    }

    private String getPropertyCaption(MetaProperty property) {
        return messageTools.getPropertyCaption(property.getDomain(), property.getName());
    }
}