package com.company.crm.ai.tool;

import com.company.crm.ai.jpql.introspection.exporter.AiDomainModelDescriptorYamlExporter;
import com.company.crm.model.base.UuidEntity;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generic Spring AI Tool for discovering and introspecting Jmix JPA entities.
 * This tool combines entity name discovery and detailed domain model introspection.
 */
public class EntitiesDiscoveryTool implements CrmAiTool {

    private static final Logger log = LoggerFactory.getLogger(EntitiesDiscoveryTool.class);

    private final MetadataTools metadataTools;
    private final AiDomainModelDescriptorYamlExporter yamlExporter;
    private final AiToolUiStatus aiToolUiStatus;
    private final Set<Class<? extends UuidEntity>> whitelist;

    public EntitiesDiscoveryTool(MetadataTools metadataTools,
                                 AiDomainModelDescriptorYamlExporter yamlExporter,
                                 AiToolUiStatus aiToolUiStatus,
                                 Collection<Class<? extends UuidEntity>> whitelist) {
        this.metadataTools = metadataTools;
        this.yamlExporter = yamlExporter;
        this.aiToolUiStatus = aiToolUiStatus;
        this.whitelist = whitelist != null ? Set.copyOf(whitelist) : Collections.emptySet();
    }

    /**
     * Get list of all available entity names.
     *
     * @return List of all JPA entity names that can be used for queries and domain model introspection.
     */
    @Tool(description = """
            Get the complete list of all available JPA entity names in the system.
            
            Use this to:
            - Explore the complete data model
            - Find specific entities by name patterns
            - Get correct entity names for detailed schema introspection
            """)
    public List<String> getAllEntityNames(ToolContext toolContext) {
        String statusStart = "Inspecting available CRM entities...";
        log.info("LLM Tool Call: getAllEntityNames()");
        aiToolUiStatus.update(toolContext, statusStart);
        List<String> names = metadataTools.getAllJpaEntityMetaClasses().stream()
                .map(MetaClass::getName)
                .sorted()
                .collect(Collectors.toList());
        aiToolUiStatus.complete(toolContext, statusStart, String.format("Discovered %d data entities", names.size()));
        return names;
    }

    /**
     * Get detailed schema information for specific entities in YAML format.
     *
     * @param entityNames List of entity names to include (e.g., ["Client", "Order"])
     * @return YAML representation of specified entities with complete schema information (fields, types, relationships).
     */
    @Tool(description = """
            Get detailed database schema information for specific entities in structured YAML format.
            
            MANDATORY: You MUST call this function for the entities you intend to query BEFORE any executeQuery() calls.
            
            Returns:
            - Exact attribute names (Java property names)
            - Entity relationships for JPQL joins
            - Property types and constraints
            - Enum properties in one map: enums.<ENUM_NAME>.id (+ optional enums.<ENUM_NAME>.description)
            
            ENUM RULE (CRITICAL):
            - When filtering enum properties in executeQuery(), use enums.<ENUM_NAME>.id, not enum constant names.
            - Example: if enums.PAID.id is 40, then pass parameter 40 (not "PAID").
            """)
    public String getDomainModelForEntities(
            @ToolParam(description = "List of entity names to include (e.g., [\"Client\", \"Order\"])")
            List<String> entityNames,
            ToolContext toolContext) {
        String statusStart = "Reading CRM schema details...";
        log.info("LLM Tool Call: getDomainModelForEntities({})", entityNames);
        aiToolUiStatus.update(toolContext, statusStart);
        try {
            // Apply whitelist filter if configured
            Set<Class<?>> requestedClasses = metadataTools.getAllJpaEntityMetaClasses().stream()
                    .filter(metaClass -> entityNames.contains(metaClass.getName()))
                    .map(MetaClass::getJavaClass)
                    .filter(clazz -> whitelist.isEmpty() || whitelist.contains(clazz))
                    .collect(Collectors.toSet());

            if (requestedClasses.isEmpty()) {
                String errorResult = "Error: No authorized or valid entity names provided. Use getAllEntityNames() to see what's available.";
                aiToolUiStatus.complete(toolContext, statusStart, "No matching entities found");
                return errorResult;
            }

            String yaml = yamlExporter.export(requestedClasses);
            List<String> matchedNames = requestedClasses.stream()
                    .map(Class::getSimpleName).sorted().collect(Collectors.toList());
            aiToolUiStatus.complete(toolContext, statusStart, String.format("Loaded schema details for %s", matchedNames));
            return yaml;
        } catch (Exception e) {
            log.error("Failed to generate domain model schema", e);
            aiToolUiStatus.complete(toolContext, statusStart, "Failed to read schema details");
            return "Error generating schema: " + e.getMessage();
        }
    }
}
