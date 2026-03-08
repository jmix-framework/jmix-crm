package com.company.crm.ai.jpql.introspection.introspector;

import com.company.crm.ai.jpql.introspection.model.AiPropertyDescriptor;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.model.MetaProperty;
import org.springframework.stereotype.Component;

/**
 * Introspects DATATYPE MetaProperties into AiProperty objects.
 */
@Component
public class DataPropertyIntrospector implements MetaPropertyIntrospector {

    private final MetadataTools metadataTools;
    private final MessageTools messageTools;

    public DataPropertyIntrospector(MetadataTools metadataTools, MessageTools messageTools) {
        this.metadataTools = metadataTools;
        this.messageTools = messageTools;
    }

    @Override
    public boolean supports(MetaProperty property) {
        return property.getType() == MetaProperty.Type.DATATYPE;
    }

    @Override
    public AiPropertyDescriptor introspect(MetaProperty property) {
        if (!supports(property)) {
            return null;
        }

        String javaType = getPropertyType(property);
        String type = property.getType().name().toLowerCase();
        Boolean identifier = isIdProperty(property) ? true : null;
        String comment = metadataTools.getMetaAnnotationValue(property, Comment.class);
        String caption = getPropertyCaption(property);

        return AiPropertyDescriptor.dataProperty(caption, comment, javaType, identifier);
    }

    private String getPropertyType(MetaProperty property) {
        return property.getJavaType().getSimpleName();
    }

    private boolean isIdProperty(MetaProperty property) {
        if (property.getAnnotatedElement().isAnnotationPresent(jakarta.persistence.Id.class) ||
            property.getAnnotatedElement().isAnnotationPresent(jakarta.persistence.EmbeddedId.class)) {
            return true;
        }

        return "id".equals(property.getName());
    }

    private String getPropertyCaption(MetaProperty property) {
        return messageTools.getPropertyCaption(property.getDomain(), property.getName());
    }
}