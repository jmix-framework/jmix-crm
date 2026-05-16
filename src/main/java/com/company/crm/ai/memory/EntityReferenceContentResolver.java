package com.company.crm.ai.memory;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.model.ChatMessageEntityReference;
import io.jmix.core.DataManager;
import io.jmix.core.EntitySerialization;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class EntityReferenceContentResolver {

    private static final Logger log = LoggerFactory.getLogger(EntityReferenceContentResolver.class);

    private final DataManager dataManager;
    private final EntitySerialization entitySerialization;
    private final IdSerialization idSerialization;
    private final FetchPlans fetchPlans;
    private final AiContextEntityRegistry contextEntityRegistry;

    public EntityReferenceContentResolver(DataManager dataManager,
                                          EntitySerialization entitySerialization,
                                          IdSerialization idSerialization,
                                          FetchPlans fetchPlans,
                                          AiContextEntityRegistry contextEntityRegistry) {
        this.dataManager = dataManager;
        this.entitySerialization = entitySerialization;
        this.idSerialization = idSerialization;
        this.fetchPlans = fetchPlans;
        this.contextEntityRegistry = contextEntityRegistry;
    }

    public String resolveContext(List<ChatMessageEntityReference> references) {
        if (references == null || references.isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder("Referenced CRM entities:");
        int resolvedCount = 0;
        for (ChatMessageEntityReference reference : references) {
            if (reference == null || !StringUtils.hasText(reference.getEntityReference())) {
                continue;
            }
            try {
                context.append("\n\n")
                        .append(reference.getEntityReference())
                        .append("\n")
                        .append(resolveEntityJson(reference.getEntityReference()));
                resolvedCount++;
            } catch (Exception e) {
                log.warn("Failed to load entity for context {}: {}", reference.getEntityReference(), e.getMessage());
            }
        }

        return resolvedCount > 0 ? context.toString() : null;
    }

    private String resolveEntityJson(String entityReference) {
        Id<Object> id = idSerialization.stringToId(entityReference);
        FetchPlan fetchPlan = contextEntityRegistry.findFetchPlan(id.getEntityClass())
                .orElseGet(() -> fetchPlans.builder(id.getEntityClass())
                        .addFetchPlan(FetchPlan.BASE)
                        .build());
        Object entity = dataManager.load(id)
                .fetchPlan(fetchPlan)
                .one();
        return entitySerialization.toJson(entity);
    }
}
