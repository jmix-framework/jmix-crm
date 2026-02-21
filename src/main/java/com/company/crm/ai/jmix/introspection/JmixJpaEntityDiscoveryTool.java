package com.company.crm.ai.jmix.introspection;

import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class JmixJpaEntityDiscoveryTool {

    private static final Logger log = LoggerFactory.getLogger(JmixJpaEntityDiscoveryTool.class);

    private final MetadataTools metadataTools;
    private final AiDomainModelDescriptorYamlExporter yamlExporter;
    private final Set<Class<?>> whitelist;

    /**
     * Creates a discovery tool with an optional whitelist.
     *
     * @param metadataTools Jmix metadata tools
     * @param yamlExporter YAML exporter for detailed schema information
     * @param whitelist Optional collection of entity classes. If provided, only these entities
     *                  will be available for schema introspection. Discovery (names only)
     *                  remains available for all entities to help the LLM navigate.
     */
    public JmixJpaEntityDiscoveryTool(MetadataTools metadataTools,
                                     AiDomainModelDescriptorYamlExporter yamlExporter,
                                     Collection<Class<?>> whitelist) {
        this.metadataTools = metadataTools;
        this.yamlExporter = yamlExporter;
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
    public List<String> getAllEntityNames() {
        log.info("LLM Tool Call: getAllEntityNames()");
        return metadataTools.getAllJpaEntityMetaClasses().stream()
                .map(MetaClass::getName)
                .sorted()
                .collect(Collectors.toList());
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
        """)
    public String getDomainModelForEntities(
            @ToolParam(description = "List of entity names to include (e.g., [\"Client\", \"Order\"])")
            List<String> entityNames) {
        log.info("LLM Tool Call: getDomainModelForEntities({})", entityNames);
        try {
            // Apply whitelist filter if configured
            Set<Class<?>> requestedClasses = metadataTools.getAllJpaEntityMetaClasses().stream()
                    .filter(metaClass -> entityNames.contains(metaClass.getName()))
                    .map(MetaClass::getJavaClass)
                    .filter(clazz -> whitelist.isEmpty() || whitelist.contains(clazz))
                    .collect(Collectors.toSet());

            if (requestedClasses.isEmpty()) {
                return "Error: No authorized or valid entity names provided. Use getAllEntityNames() to see what's available.";
            }

            return yamlExporter.export(requestedClasses);
        } catch (Exception e) {
            log.error("Failed to generate domain model schema", e);
            return "Error generating schema: " + e.getMessage();
        }
    }
}
