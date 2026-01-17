package com.company.crm.app.service.ai;

import com.company.crm.ai.jmix.introspection.AiDomainModelDescriptorYamlExporter;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemComment;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring AI Tool for CRM domain model introspection.
 * Provides detailed schema information about CRM entities using the AI Domain Model Descriptor YAML Exporter.
 * This tool replaces the generic getEntityHelp method with CRM-specific domain model information.
 */
@Component("crm_CrmDomainModelIntrospectionTool")
public class CrmDomainModelIntrospectionTool {

    private static final Logger log = LoggerFactory.getLogger(CrmDomainModelIntrospectionTool.class);

    /**
     * CRM-specific entities that should be available for AI queries.
     * This whitelist excludes Jmix framework entities and focuses on business domain entities.
     */
    private static final Set<Class<?>> CRM_ENTITIES = Set.of(
        Client.class,
        Order.class,
        OrderItem.class,
        Category.class,
        CategoryItem.class,
        CategoryItemComment.class,
        Invoice.class,
        Payment.class,
        User.class,
        Contact.class
    );

    private final AiDomainModelDescriptorYamlExporter yamlExporter;
    private final MetadataTools metadataTools;

    public CrmDomainModelIntrospectionTool(AiDomainModelDescriptorYamlExporter yamlExporter,
                                          MetadataTools metadataTools) {
        this.yamlExporter = yamlExporter;
        this.metadataTools = metadataTools;
    }

    /**
     * Get comprehensive CRM domain model schema information
     *
     * @return YAML representation of CRM entities with complete schema information
     */
    @Tool(description = """
        MANDATORY FIRST CALL: Get complete CRM database schema information in structured YAML format.

        You MUST call this function BEFORE any executeQuery() calls to obtain:
        - All CRM entity names and their exact attribute names
        - All entity relationships for JPQL joins
        - Complete property types and constraints
        - Entity hierarchy and inheritance information
        - JPA mapping details optimized for AI consumption

        WITHOUT calling this function first, executeQuery() will fail because you won't know:
        - Correct entity names (e.g., 'Order_' not 'Order')
        - Correct attribute names (Java property names, not database columns)
        - Available entity relationships for joins
        - Property types for proper parameter binding

        The returned YAML format is optimized for AI systems and provides structured,
        machine-readable domain model information that complements JPQL query execution.

        This is a PREREQUISITE - call this function at least once before attempting any database queries.
        """)
    public String getCrmDomainModel() {
        log.info("LLM Tool Call: getCrmDomainModel() - Providing CRM domain model schema");
        try {
            String yamlSchema = yamlExporter.export(CRM_ENTITIES);
            log.info("Successfully generated CRM domain model YAML schema, size: {} characters", yamlSchema.length());
            return yamlSchema;
        } catch (Exception e) {
            log.error("Failed to generate CRM domain model schema", e);
            return "Error generating CRM domain model schema: " + e.getMessage();
        }
    }

    /**
     * Get schema information for specific CRM entities
     *
     * @param entityNames List of entity names to include (e.g., ["Client", "Order", "Invoice"])
     * @return YAML representation of specified entities with complete schema information
     */
    @Tool(description = """
        Get CRM domain model schema information for specific entities only.

        This is useful when you need detailed schema information for only a subset of entities
        to reduce response size and focus on specific domain areas.

        IMPORTANT: Use EntityListTool.getAllEntityNames() first to get the list of all available entity names.

        Returns the same detailed YAML format as getCrmDomainModel() but only for requested entities.
        Use this when you know exactly which entities you need to work with.
        """)
    public String getCrmDomainModelForEntities(
            @ToolParam(description = "List of entity names to include (e.g., [\"Client\", \"Order\", \"Invoice\"])")
            List<String> entityNames) {
        log.info("LLM Tool Call: getCrmDomainModelForEntities() - Providing schema for entities: {}", entityNames);
        try {
            // Convert entity names to classes using MetadataTools
            Set<Class<?>> requestedEntities = metadataTools.getAllJpaEntityMetaClasses().stream()
                .filter(metaClass -> entityNames.contains(metaClass.getName()))
                .filter(metaClass -> CRM_ENTITIES.contains(metaClass.getJavaClass()))
                .map(MetaClass::getJavaClass)
                .collect(Collectors.toSet());

            if (requestedEntities.isEmpty()) {
                log.warn("No valid CRM entities found for names: {}", entityNames);
                return "Error: No valid CRM entity names provided. Use EntityListTool.getAllEntityNames() to get available entities.";
            }

            String yamlSchema = yamlExporter.export(requestedEntities);
            log.info("Successfully generated CRM domain model YAML schema for {} entities, size: {} characters",
                    requestedEntities.size(), yamlSchema.length());
            return yamlSchema;
        } catch (Exception e) {
            log.error("Failed to generate CRM domain model schema for entities: {}", entityNames, e);
            return "Error generating CRM domain model schema for specified entities: " + e.getMessage();
        }
    }

}