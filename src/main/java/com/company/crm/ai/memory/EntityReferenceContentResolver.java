package com.company.crm.ai.memory;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.model.ChatMessageEntityReference;
import io.jmix.core.DataManager;
import io.jmix.core.EntitySerialization;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Component
public class EntityReferenceContentResolver {

    private static final Logger log = LoggerFactory.getLogger(EntityReferenceContentResolver.class);

    private final DataManager dataManager;
    private final EntitySerialization entitySerialization;
    private final IdSerialization idSerialization;
    private final AiContextEntityRegistry contextEntityRegistry;

    public EntityReferenceContentResolver(DataManager dataManager,
                                          EntitySerialization entitySerialization,
                                          IdSerialization idSerialization,
                                          AiContextEntityRegistry contextEntityRegistry) {
        this.dataManager = dataManager;
        this.entitySerialization = entitySerialization;
        this.idSerialization = idSerialization;
        this.contextEntityRegistry = contextEntityRegistry;
    }

    public String resolveContext(List<ChatMessageEntityReference> references) {
        if (references == null || references.isEmpty()) {
            return null;
        }

        // TODO: extract method
        List<String> resolvedJsons = references.stream()
                .filter(ref -> ref != null && StringUtils.hasText(ref.getEntityReference()))
                .map(ref -> {
                    try {
                        String json = resolveEntityJson(ref.getEntityReference());
                        return """
                                %s
                                %s""".formatted(ref.getEntityReference(), json);
                    } catch (Exception e) {
                        log.warn("Failed to load entity for context {}: {}", ref.getEntityReference(), e.getMessage());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        if (resolvedJsons.isEmpty()) {
            return null;
        }

        return """
               Referenced CRM entities:

               %s""".formatted(String.join("\n\n", resolvedJsons));
    }

    private String resolveEntityJson(String entityReference) {
        Id<Object> id = idSerialization.stringToId(entityReference);
        FetchPlan fetchPlan = contextEntityRegistry.findFetchPlan(id.getEntityClass()).orElseThrow();
        Object entity = dataManager.load(id)
                .fetchPlan(fetchPlan)
                .one();
        return entitySerialization.toJson(entity);
    }
}
