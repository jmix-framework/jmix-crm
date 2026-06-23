package com.company.crm.ai.tool;

import com.company.crm.ai.context.AiContextEntityRegistry;
import io.jmix.aitools.dataload.introspection.impl.DefaultAvailableEntityFilter;
import io.jmix.aitools.dataload.introspection.model.EntityDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Restricts the entities the AI data-load tools (domain-model discovery and JPQL execution) may see to
 * the curated CRM allowlist ({@link AiContextEntityRegistry#aiToolContextEntityDefinitions()}),
 * on top of the Jmix CRUD read-access check inherited from {@link DefaultAvailableEntityFilter}.
 */
@Component
public class CrmAvailableEntityFilter extends DefaultAvailableEntityFilter {

    private final AiContextEntityRegistry contextEntityRegistry;

    public CrmAvailableEntityFilter(AiContextEntityRegistry contextEntityRegistry) {
        this.contextEntityRegistry = contextEntityRegistry;
    }

    @Override
    public List<EntityDescriptor> filter(List<EntityDescriptor> entityDescriptors) {
        Set<String> allowedEntityNames = contextEntityRegistry.aiToolContextEntityDefinitions().stream()
                .map(definition -> metadata.getClass(definition.entityClass()).getName())
                .collect(Collectors.toSet());

        return super.filter(entityDescriptors).stream()
                .filter(descriptor -> allowedEntityNames.contains(descriptor.getName()))
                .toList();
    }
}
