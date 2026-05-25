package com.company.crm.ai.context;

import com.company.crm.model.base.UuidEntity;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AiContextEntityRegistry {

    private final FetchPlans fetchPlans;

    public AiContextEntityRegistry(FetchPlans fetchPlans) {
        this.fetchPlans = fetchPlans;
    }

    // TODO: remove
    public List<AiContextEntityDefinition> definitions() {
        return List.of(AiContextEntityDefinition.values());
    }

    public List<AiContextEntityDefinition> addMenuDefinitions() {
        return Arrays.stream(AiContextEntityDefinition.values())
                .filter(AiContextEntityDefinition::addMenuVisible)
                .toList();
    }

    // TODO: find why the set needs to be returned of if the answer can be answered within this enum?
    public Set<Class<? extends UuidEntity>> toolEntityClasses() {
        return Arrays.stream(AiContextEntityDefinition.values())
                .filter(AiContextEntityDefinition::toolsAllowed)
                .map(AiContextEntityDefinition::entityClass)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<AiContextEntityDefinition> findDefinition(Class<?> entityClass) {
        return AiContextEntityDefinition.findByEntityClass(entityClass);
    }

    public Optional<FetchPlan> findFetchPlan(Class<?> entityClass) {
        return findDefinition(entityClass)
                .map(definition -> definition.fetchPlan(fetchPlans));
    }
}
