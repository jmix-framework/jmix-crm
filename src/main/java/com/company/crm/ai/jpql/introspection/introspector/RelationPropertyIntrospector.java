package com.company.crm.ai.jpql.introspection.introspector;

import com.company.crm.ai.jpql.introspection.model.AiPropertyDescriptor;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

/**
 * Introspects ASSOCIATION and COMPOSITION MetaProperties into AiProperty objects.
 */
@Component
public class RelationPropertyIntrospector implements MetaPropertyIntrospector {

    @Autowired
    private MetadataTools metadataTools;

    @Autowired
    private MessageTools messageTools;

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

        return AiPropertyDescriptor.relationProperty(
                getPropertyCaption(property, messageTools),
                metadataTools.getMetaAnnotationValue(property, Comment.class),
                property.getRange().getCardinality().name(),
                property.getRange().asClass().getName(),
                property.getRange().asClass().getName(),
                isOptionalRelation(property) ? null : false,
                getMappedByValue(property)
        );
    }

    private boolean isOptionalRelation(MetaProperty property) {
        if (property.isMandatory()) {
            return false;
        }

        ManyToOne manyToOne = property.getAnnotatedElement().getAnnotation(ManyToOne.class);
        if (manyToOne != null) {
            return manyToOne.optional();
        }

        OneToOne oneToOne = property.getAnnotatedElement().getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            return oneToOne.optional();
        }

        return true;
    }

    private String getMappedByValue(MetaProperty property) {
        return Stream.of(
                        mappedByFromOneToMany(property),
                        mappedByFromOneToOne(property),
                        mappedByFromManyToMany(property),
                        mappedByFromComposition(property)
                )
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String mappedByFromOneToMany(MetaProperty property) {
        OneToMany ann = property.getAnnotatedElement().getAnnotation(OneToMany.class);
        return ann != null && StringUtils.hasText(ann.mappedBy()) ? ann.mappedBy() : null;
    }

    private String mappedByFromOneToOne(MetaProperty property) {
        OneToOne ann = property.getAnnotatedElement().getAnnotation(OneToOne.class);
        return ann != null && StringUtils.hasText(ann.mappedBy()) ? ann.mappedBy() : null;
    }

    private String mappedByFromManyToMany(MetaProperty property) {
        ManyToMany ann = property.getAnnotatedElement().getAnnotation(ManyToMany.class);
        return ann != null && StringUtils.hasText(ann.mappedBy()) ? ann.mappedBy() : null;
    }

    private String mappedByFromComposition(MetaProperty property) {
        Composition ann = property.getAnnotatedElement().getAnnotation(Composition.class);
        return ann != null && StringUtils.hasText(ann.inverse()) ? ann.inverse() : null;
    }
}