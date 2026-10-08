package com.company.crm.ai.context;

import io.jmix.aitools.ExcludeFromAi;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanBuilder;
import io.jmix.core.FetchPlanProperty;
import io.jmix.core.FetchPlans;
import io.jmix.core.Metadata;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class AiContextEntityRegistry {

    private final FetchPlans fetchPlans;
    private final Metadata metadata;

    public AiContextEntityRegistry(FetchPlans fetchPlans, Metadata metadata) {
        this.fetchPlans = fetchPlans;
        this.metadata = metadata;
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

    /**
     * Returns the fetch plan an entity added to the chat context is loaded and serialized for the model with.
     * Attributes marked {@link ExcludeFromAi} are left out at every level, so the context the CRM itself puts
     * into the prompt respects the same boundary as the AI Tools data-load queries.
     */
    public Optional<FetchPlan> findFetchPlan(Class<?> entityClass) {
        return findDefinition(entityClass)
                .map(definition -> definition.fetchPlan(fetchPlans))
                .map(this::withoutExcludedFromAi);
    }

    /**
     * Whether the attribute is marked {@link ExcludeFromAi}, so the CRM must not hand its value to the model.
     */
    public static boolean isExcludedFromAi(MetaProperty property) {
        return property.getAnnotations().containsKey(ExcludeFromAi.class.getName());
    }

    private FetchPlan withoutExcludedFromAi(FetchPlan fetchPlan) {
        FetchPlanBuilder builder = fetchPlans.builder(fetchPlan.getEntityClass());
        addAllowedProperties(builder, fetchPlan);
        return builder.build();
    }

    private void addAllowedProperties(FetchPlanBuilder builder, FetchPlan fetchPlan) {
        MetaClass metaClass = metadata.getClass(fetchPlan.getEntityClass());
        for (FetchPlanProperty property : fetchPlan.getProperties()) {
            if (isExcludedFromAi(metaClass.getProperty(property.getName()))) {
                continue;
            }
            FetchPlan nested = property.getFetchPlan();
            if (nested == null) {
                builder.add(property.getName());
            } else {
                builder.add(property.getName(), b -> addAllowedProperties(b, nested), property.getFetchMode());
            }
        }
    }
}
