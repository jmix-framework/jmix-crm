package com.company.crm.ai.context;

import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class AiContextEntityRegistry {

    private final FetchPlans fetchPlans;

    public AiContextEntityRegistry(FetchPlans fetchPlans) {
        this.fetchPlans = fetchPlans;
    }

    public List<AiContextEntityDefinition> addMenuDefinitions() {
        return Arrays.stream(AiContextEntityDefinition.values())
                .filter(AiContextEntityDefinition::addMenuVisible)
                .toList();
    }

    /**
     * Returns the CRM context entity definitions that AI tools are allowed to discover and search.
     */
    public List<AiContextEntityDefinition> aiToolContextEntityDefinitions() {
        return Arrays.stream(AiContextEntityDefinition.values())
                .filter(AiContextEntityDefinition::toolsAllowed)
                .toList();
    }

    public Optional<AiContextEntityDefinition> findDefinition(Class<?> entityClass) {
        return AiContextEntityDefinition.findByEntityClass(entityClass);
    }

    public Optional<FetchPlan> findFetchPlan(Class<?> entityClass) {
        return findDefinition(entityClass)
                .map(definition -> definition.fetchPlan(fetchPlans));
    }
}
