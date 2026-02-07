package com.company.crm.ai.jmix.introspection;

import com.company.crm.ai.jmix.introspection.introspector.JpaDomainModelIntrospector;
import com.company.crm.ai.jmix.introspection.model.AiDomainModelDescriptor;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main public API for exporting AI domain model descriptors to YAML.
 * This is the primary entry point for users of this functionality.
 */
@Component("ai_AiDomainModelDescriptorYamlExporter")
public class AiDomainModelDescriptorYamlExporter {

    private final JpaDomainModelIntrospector introspector;
    private final AiDomainModelDescriptorYamlWriter yamlWriter;
    private final MetadataTools metadataTools;

    public AiDomainModelDescriptorYamlExporter(JpaDomainModelIntrospector introspector,
                                              AiDomainModelDescriptorYamlWriter yamlWriter,
                                              MetadataTools metadataTools) {
        this.introspector = introspector;
        this.yamlWriter = yamlWriter;
        this.metadataTools = metadataTools;
    }

    /**
     * Export the current domain model as AI descriptors to YAML format.
     *
     * @return YAML string representation of all JPA entities optimized for AI systems
     */
    public String export() {
        AiDomainModelDescriptor domainModel = introspector.introspect();
        return yamlWriter.writeToYaml(domainModel);
    }

    /**
     * Export only specified entity classes as AI descriptors to YAML format.
     * This allows filtering out unwanted Jmix framework entities and focusing on specific domain classes.
     * More efficient than introspecting all entities and then filtering.
     *
     * @param entityClasses set of entity classes to include
     * @return YAML string representation of only the specified entities optimized for AI systems
     */
    public String export(Set<Class<?>> entityClasses) {
        Collection<MetaClass> metaClasses = metadataTools.getAllJpaEntityMetaClasses().stream()
                .filter(metaClass -> entityClasses.contains(metaClass.getJavaClass()))
                .collect(Collectors.toList());

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);
        return yamlWriter.writeToYaml(domainModel);
    }
}