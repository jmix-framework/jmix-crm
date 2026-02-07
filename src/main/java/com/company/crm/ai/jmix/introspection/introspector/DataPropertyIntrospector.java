package com.company.crm.ai.jmix.introspection.introspector;

import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
import io.jmix.core.Messages;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import org.springframework.stereotype.Component;

/**
 * Introspects DATATYPE MetaProperties into AiProperty objects.
 */
@Component
public class DataPropertyIntrospector implements MetaPropertyIntrospector {

    private final MetadataTools metadataTools;
    private final Messages messages;
    private final MessageTools messageTools;

    public DataPropertyIntrospector(MetadataTools metadataTools, Messages messages, MessageTools messageTools) {
        this.metadataTools = metadataTools;
        this.messages = messages;
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
        Class<?> javaType = property.getJavaType();
        if (javaType != null) {
            return javaType.getSimpleName();
        }

        Range range = property.getRange();
        if (range.isDatatype()) {
            return range.asDatatype().getJavaClass().getSimpleName();
        }

        return "Object";
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