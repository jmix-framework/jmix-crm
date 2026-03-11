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

        return AiPropertyDescriptor.dataProperty(
                messageTools.getPropertyCaption(property.getDomain(), property.getName()),
                metadataTools.getMetaAnnotationValue(property, Comment.class),
                property.getJavaType().getSimpleName(),
                property.equals(metadataTools.getPrimaryKeyProperty(property.getDomain())) ? true : null
        );
    }
}