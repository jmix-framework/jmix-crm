package com.company.crm.ai.jpql.introspection.introspector;

import com.company.crm.ai.jpql.introspection.model.AiDomainModelDescriptor;
import com.company.crm.ai.jpql.introspection.model.AiEntityDescriptor;
import com.company.crm.ai.jpql.introspection.model.AiPropertyDescriptor;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Introspects Jmix metadata and converts it to domain model DTOs.
 */
@Component
public class JpaDomainModelIntrospector {

    private final MetadataTools metadataTools;
    private final MessageTools messageTools;
    private final List<MetaPropertyIntrospector> introspectors;

    public JpaDomainModelIntrospector(MetadataTools metadataTools, MessageTools messageTools, List<MetaPropertyIntrospector> introspectors) {
        this.metadataTools = metadataTools;
        this.messageTools = messageTools;
        this.introspectors = introspectors;
    }

    public AiDomainModelDescriptor introspect() {
        return introspect(metadataTools.getAllJpaEntityMetaClasses());
    }

    /**
     * Introspect only specified MetaClasses.
     * More efficient than introspecting all entities and then filtering.
     *
     * @param metaClasses collection of MetaClasses to introspect
     * @return AiDomainModelDescriptor containing only the specified entities
     */
    public AiDomainModelDescriptor introspect(Collection<MetaClass> metaClasses) {
        Map<String, AiEntityDescriptor> entities = metaClasses.stream()
                .filter(metadataTools::isJpaEntity)
                .collect(LinkedHashMap::new,
                        (introspect, metaClass) -> introspect.put(metaClass.getName(), introspectEntity(metaClass)),
                        Map::putAll);

        return new AiDomainModelDescriptor(entities);
    }

    public AiEntityDescriptor introspectEntity(MetaClass metaClass) {

        return new AiEntityDescriptor(
                messageTools.getEntityCaption(metaClass),
                metadataTools.getMetaAnnotationValue(metaClass, Comment.class),
                introspectProperties(metaClass)
        );
    }

    private LinkedHashMap<String, AiPropertyDescriptor> introspectProperties(MetaClass metaClass) {
        return metaClass.getProperties().stream()
                .filter(metadataTools::isJpa)
                .collect(LinkedHashMap::new,
                        (map, property) -> introspectProperty(property)
                                .ifPresent(propertyModel -> map.put(property.getName(), propertyModel)),
                        Map::putAll);
    }

    private Optional<AiPropertyDescriptor> introspectProperty(MetaProperty property) {
        return findIntrospector(property)
                .map(introspector -> introspector.introspect(property));
    }

    private Optional<MetaPropertyIntrospector> findIntrospector(MetaProperty property) {
        return introspectors.stream()
                .filter(introspector -> introspector.supports(property))
                .findFirst();
    }

}
