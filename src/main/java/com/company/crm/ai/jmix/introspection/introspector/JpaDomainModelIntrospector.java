package com.company.crm.ai.jmix.introspection.introspector;

import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiDomainModelDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiEntityDescriptor;
import io.jmix.core.Messages;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.model.MetaClass;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Introspects Jmix metadata and converts it to domain model DTOs.
 */
@Component("ai_JpaDomainModelIntrospector")
public class JpaDomainModelIntrospector {


    private final MetadataTools metadataTools;
    private final Messages messages;
    private final MessageTools messageTools;
    private final List<MetaPropertyIntrospector> introspectors;

    public JpaDomainModelIntrospector(MetadataTools metadataTools, Messages messages, MessageTools messageTools, List<MetaPropertyIntrospector> introspectors) {
        this.metadataTools = metadataTools;
        this.messages = messages;
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
                        (map, property) -> introspectors.stream()
                                .filter(introspector -> introspector.supports(property))
                                .findFirst()
                                .map(introspector -> introspector.introspect(property))
                                .ifPresent(propertyModel -> map.put(property.getName(), propertyModel)),
                        Map::putAll);
    }

}