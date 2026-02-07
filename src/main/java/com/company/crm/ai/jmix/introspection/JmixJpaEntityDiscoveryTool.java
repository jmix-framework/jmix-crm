package com.company.crm.ai.jmix.introspection;

import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI Tool for discovering all available entity names in the Jmix application.
 * Provides a comprehensive list of all JPA entity names via Jmix Metadata API.
 */
@Component("ai_JmixJpaEntityDiscoveryTool")
public class JmixJpaEntityDiscoveryTool {

    private static final Logger log = LoggerFactory.getLogger(JmixJpaEntityDiscoveryTool.class);

    private final MetadataTools metadataTools;

    public JmixJpaEntityDiscoveryTool(MetadataTools metadataTools) {
        this.metadataTools = metadataTools;
    }

    /**
     * Get list of all available entity names
     *
     * @return List of all JPA entity names that can be used for queries and domain model introspection
     */
    @Tool(description = """
        Get the complete list of all available JPA entity names in the system.

        This tool provides the definitive list of all entity names that are available for:
        - JPQL query construction
        - Domain model introspection
        - Understanding the complete data model

        Returns all entity names derived from the actual JPA metadata, ensuring accuracy.
        This includes both business entities and framework entities.

        Use this when you need to:
        - Explore the complete data model
        - Find specific entities by name patterns
        - Understand the full scope of available entities
        - Get entity names for domain model tools
        """)
    public List<String> getAllEntityNames() {
        log.info("LLM Tool Call: getAllEntityNames() - Providing list of all JPA entities");
        try {
            List<String> entityNames = metadataTools.getAllJpaEntityMetaClasses().stream()
                .map(MetaClass::getName)
                .sorted()
                .collect(Collectors.toList());

            log.info("Successfully generated entity names list, count: {}", entityNames.size());
            log.debug("All entities: {}", entityNames);

            return entityNames;
        } catch (Exception e) {
            log.error("Failed to generate entity names list", e);
            throw new RuntimeException("Error generating entity names list: " + e.getMessage(), e);
        }
    }
}