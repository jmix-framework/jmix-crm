package com.company.crm.ai.memory;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.model.ChatMessageEntityReference;
import com.company.crm.ai.prompt.AiPromptContentBuilder;
import io.jmix.core.DataManager;
import io.jmix.core.EntitySerialization;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

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
        if (CollectionUtils.isEmpty(references)) {
            return null;
        }

        List<String> referenceBlocks = resolveReferenceBlocks(references);

        if (referenceBlocks.isEmpty()) {
            return null;
        }

        return AiPromptContentBuilder.create()
                .appendSection("Referenced CRM entities", referenceBlocks)
                .build();
    }

    private List<String> resolveReferenceBlocks(List<ChatMessageEntityReference> references) {
        return references.stream()
                .map(this::resolveReferenceBlock)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<String> resolveReferenceBlock(ChatMessageEntityReference reference) {
        if (reference == null || !StringUtils.hasText(reference.getEntityReference())) {
            return Optional.empty();
        }

        String entityReference = reference.getEntityReference();
        try {
            return Optional.of(formatReferenceBlock(entityReference, resolveReferenceJson(entityReference)));
        } catch (Exception e) {
            log.warn("Failed to load entity for context {}: {}", entityReference, e.getMessage());
            return Optional.empty();
        }
    }

    private String formatReferenceBlock(String entityReference, String json) {
        return AiPromptContentBuilder.create()
                .appendParagraph(entityReference)
                .appendParagraph(json)
                .build();
    }

    private String resolveReferenceJson(String entityReference) {
        Id<Object> id = idSerialization.stringToId(entityReference);
        FetchPlan fetchPlan = contextEntityRegistry.findFetchPlan(id.getEntityClass()).orElseThrow();
        Object entity = dataManager.load(id)
                .fetchPlan(fetchPlan)
                .one();
        return entitySerialization.toJson(entity);
    }
}
