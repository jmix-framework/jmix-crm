package com.company.crm.ai.jmix.introspection.introspector;

import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Introspects ASSOCIATION and COMPOSITION MetaProperties into AiProperty objects.
 */
@Component
public class RelationPropertyIntrospector implements MetaPropertyIntrospector {

    @Autowired
    private MetadataTools metadataTools;

    @Override
    public boolean supports(MetaProperty property) {
        MetaProperty.Type type = property.getType();
        return (type == MetaProperty.Type.ASSOCIATION || type == MetaProperty.Type.COMPOSITION) &&
                property.getRange().getCardinality() != Range.Cardinality.NONE;
    }

    @Override
    public AiPropertyDescriptor introspect(MetaProperty property) {
        if (!supports(property)) {
            return null;
        }

        Range range = property.getRange();
        Range.Cardinality cardinality = range.getCardinality();

        String javaType = range.asClass().getName();
        String type = cardinality.name();
        String target = range.asClass().getName();
        Boolean optional = isOptionalRelation(property) ? null : false;
        String mappedBy = getMappedByValue(property);
        String comment = metadataTools.getMetaAnnotationValue(property, Comment.class);

        return AiPropertyDescriptor.relationProperty(null, comment, type, javaType, target, optional, mappedBy);
    }


    private boolean isOptionalRelation(MetaProperty property) {
        if (property.isMandatory()) {
            return false;
        }

        jakarta.persistence.ManyToOne manyToOne = property.getAnnotatedElement().getAnnotation(jakarta.persistence.ManyToOne.class);
        if (manyToOne != null) {
            return manyToOne.optional();
        }

        jakarta.persistence.OneToOne oneToOne = property.getAnnotatedElement().getAnnotation(jakarta.persistence.OneToOne.class);
        if (oneToOne != null) {
            return oneToOne.optional();
        }

        return true;
    }

    private String getMappedByValue(MetaProperty property) {
        jakarta.persistence.OneToMany oneToMany = property.getAnnotatedElement().getAnnotation(jakarta.persistence.OneToMany.class);
        if (oneToMany != null && !oneToMany.mappedBy().isBlank()) {
            return oneToMany.mappedBy();
        }

        jakarta.persistence.OneToOne oneToOne = property.getAnnotatedElement().getAnnotation(jakarta.persistence.OneToOne.class);
        if (oneToOne != null && !oneToOne.mappedBy().isBlank()) {
            return oneToOne.mappedBy();
        }

        jakarta.persistence.ManyToMany manyToMany = property.getAnnotatedElement().getAnnotation(jakarta.persistence.ManyToMany.class);
        if (manyToMany != null && !manyToMany.mappedBy().isBlank()) {
            return manyToMany.mappedBy();
        }

        io.jmix.core.metamodel.annotation.Composition composition = property.getAnnotatedElement().getAnnotation(io.jmix.core.metamodel.annotation.Composition.class);
        if (composition != null && !composition.inverse().isBlank()) {
            return composition.inverse();
        }

        return null;
    }
}