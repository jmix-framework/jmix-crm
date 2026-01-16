package com.company.crm.app.service.ai;

import com.company.crm.app.service.query.JpqlQueryService;
import com.company.crm.app.service.query.QueryExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI Tool for executing JPQL queries against the CRM database
 * Uses @Tool annotation pattern like in the insurance-ai project
 */
@Component("crm_JpqlQueryTool")
public class JpqlQueryTool {

    private static final Logger log = LoggerFactory.getLogger(JpqlQueryTool.class);

    @Autowired
    private JpqlQueryService jpqlQueryService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String entityHelpContent;

    @Autowired
    public JpqlQueryTool(@Value("classpath:prompts/entity-help-prompt.st") Resource entityHelpPrompt) {
        try {
            this.entityHelpContent = StreamUtils.copyToString(
                    entityHelpPrompt.getInputStream(),
                    StandardCharsets.UTF_8
            );
            log.info("Entity help content loaded from entity-help-prompt.st");
        } catch (IOException e) {
            log.error("Failed to load entity help content from entity-help-prompt.st", e);
            throw new RuntimeException("Could not load entity help content", e);
        }
    }

    /**
     * Execute a JPQL query with parameters
     *
     * @param jpqlQuery The JPQL query to execute against the CRM database. Use proper entity names like 'Client', 'Order_', 'OrderItem', etc.
     * @param parameters Named parameters for the query.
     * @return Query execution result with data or error message
     */
    @Tool(description = """
        Execute JPQL queries against the CRM database to retrieve business insights.

        CRITICAL REQUIREMENTS:
        1. MUST call getEntityHelp-Tool first to get complete entity schema and JPQL guidelines
        2. MUST use AS aliases for ALL SELECT fields
        3. MUST provide the selectAliases parameter with all aliases in order
        4. Use exact entity names and attribute names from the schema

        ALIAS REQUIREMENT:
        ✓ CORRECT: SELECT c.name AS clientName, COUNT(o) AS orderCount FROM Client c LEFT JOIN c.orders o GROUP BY c
        Then provide: selectAliases = ["clientName", "orderCount"]

        ✗ INCORRECT: SELECT c.name, COUNT(o) FROM Client c LEFT JOIN c.orders o GROUP BY c
        (Missing AS aliases and selectAliases parameter)

        Without getEntityHelp-Tool first, queries may fail due to incorrect entity/attribute names.
        Use standard date parameters or ISO dates like '2024-01-01'
        """)
    public CrmQueryResult executeQuery(
            @ToolParam(description = "JPQL query with AS aliases for all SELECT fields") String jpqlQuery,
            @ToolParam(description = "Named parameters for the query (empty map if none)") Map<String, Object> parameters,
            @ToolParam(description = "List of aliases used in SELECT clause, in order (e.g., ['clientName', 'orderCount'])") List<String> selectAliases) {
        try {
            log.info("LLM Tool Call: executeQuery() - JPQL: {}", jpqlQuery);

            if (parameters == null) {
                parameters = new HashMap<>();
            }

            QueryExecutionResult result = jpqlQueryService.executeJpqlQuery(jpqlQuery, parameters, selectAliases);

            if (result.success()) {
                log.info("Query Result: {} rows returned", result.rowCount());

                return new CrmQueryResult(
                        true,
                        toJson(result.data()),
                        result.rowCount(),
                        null,
                        result.executedQuery(),
                        result.parameters()
                );
            } else {
                log.warn("Query Failed: {}", result.errorMessage());
                return new CrmQueryResult(
                        false,
                        "[]",
                        0,
                        result.errorMessage(),
                        result.executedQuery(),
                        result.parameters()
                );
            }

        } catch (Exception e) {
            log.error("Query Error: {} - {}", jpqlQuery, e.getMessage());
            return new CrmQueryResult(
                    false,
                    "[]",
                    0,
                    "Error executing query: " + e.getMessage(),
                    jpqlQuery,
                    parameters != null ? parameters : new HashMap<>()
            );
        }
    }

    private String toJson(List<Map<String, Object>> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize query result to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * Get help for available entities and relationships
     *
     * @return Information about available CRM entities loaded from entity-help-prompt.st
     */
    @Tool(description = """
        MANDATORY FIRST CALL: Get complete CRM database schema information.

        You MUST call this function BEFORE any executeQuery() calls to obtain:
        - All entity names and their exact Java attribute names
        - All entity relationships for JPQL joins
        - Complete JPQL syntax rules and Jmix extensions
        - All JPQL restrictions and forbidden operations
        - Correct query examples and patterns

        WITHOUT calling this function first, executeQuery() will fail because you won't know:
        - Correct entity names (e.g., 'Order_' not 'Order')
        - Correct attribute names (Java names, not database columns)
        - Required JPQL syntax (no SELECT *, no aliases, etc.)
        - Available Jmix JPQL functions and limitations

        This is a PREREQUISITE - call this function at least once before attempting any database queries.
        """)
    public String getEntityHelp() {
        log.info("LLM Tool Call: getEntityHelp() - Providing schema information");
        return entityHelpContent;
    }
}