package com.company.crm.ai.jmix.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Spring AI Tool for executing JPQL queries against JPA databases
 * Uses @Tool annotation pattern - generic implementation for Jmix applications
 */
@Component("ai_JpqlQueryTool")
public class JpqlQueryTool {

    private static final Logger log = LoggerFactory.getLogger(JpqlQueryTool.class);

    private final AiJpqlQueryService aiJpqlQueryService;

    public JpqlQueryTool(AiJpqlQueryService aiJpqlQueryService) {
        this.aiJpqlQueryService = aiJpqlQueryService;
    }

    /**
     * Execute a JPQL query with parameters
     *
     * @param jpqlQuery The JPQL query to execute against the CRM database. Use proper entity names like 'Client', 'Order_', 'OrderItem', etc.
     * @param parameters Named parameters for the query.
     * @return Query execution result with data or error message
     */
    @Tool(description = """
        Execute JPQL queries against the JPA database to retrieve insights.

        CRITICAL REQUIREMENTS:
        1. MUST call domain model tools first to get complete entity schema information
        2. MUST use AS aliases for ALL SELECT fields
        3. MUST provide the selectAliases parameter with all aliases in order
        4. Use exact entity names and attribute names from the schema

        ALIAS REQUIREMENT:
        ✓ CORRECT: SELECT c.name AS clientName, COUNT(o) AS orderCount FROM Client c LEFT JOIN c.orders o GROUP BY c
        Then provide: selectAliases = ["clientName", "orderCount"]

        ✗ INCORRECT: SELECT c.name, COUNT(o) FROM Client c LEFT JOIN c.orders o GROUP BY c
        (Missing AS aliases and selectAliases parameter)

        JPQL SYNTAX RULES (Jmix/EclipseLink):
        - Use entity names not table names
        - Use attribute names not column names
        - Use entity relationships for joins not foreign keys
        - No SELECT * allowed - specify exact attributes
        - Use COUNT(entity) not COUNT(*)
        - No aliases in SELECT for security (AS aliases required for this tool only)
        - No subqueries in SELECT clause
        - Use GROUP BY entity not GROUP BY entity.id

        DATE HANDLING - TWO OPTIONS:

        OPTION 1 - JMIX MACROS (RECOMMENDED for date ranges):
        Jmix provides powerful date macros that handle current time calculations:

        @between(field, start, end, unit) - Date range queries:
        Examples:
        - Last 30 days: @between(o.date, now-30, now, day)
        - Last month: @between(o.date, now-1, now, month)
        - Today only: @today(o.date)
        - Yesterday: @dateEquals(o.date, now-1)
        - Future dates: @dateAfter(o.date, now)
        - Past dates: @dateBefore(o.date, now-7)

        ✓ CORRECT for last 30 days:
        SELECT o.number AS orderNumber, o.total AS orderTotal FROM Order_ o WHERE @between(o.date, now-30, now, day)

        ✗ INCORRECT: Don't use CURRENT_DATE arithmetic like "CURRENT_DATE - 30"

        OPTION 2 - LITERAL DATES:
        Use ISO date literals in single quotes for fixed dates:
        - WHERE o.date >= '2024-01-01'
        - WHERE o.date BETWEEN '2024-01-01' AND '2024-01-31'

        PARAMETER HANDLING:
        Parameters are automatically converted to appropriate types when possible:
        - Date strings → LocalDate/LocalDateTime (e.g., "2024-01-15")
        - Numeric strings → BigDecimal/Integer/Long (e.g., "1500.50", "42")
        - UUID strings → UUID for entity IDs
        - Boolean strings → Boolean ("true", "false")
        - Other strings remain as strings (LIKE patterns, etc.)

        Examples:
        ✓ Parameters: {"startDate": "2024-01-15", "minValue": "1000.00", "pattern": "%Test%"}
        ✓ Macros: @between(o.date, now-30, now, day)

        JMIX JPQL EXTENSIONS:
        - Date functions: YEAR(date), MONTH(date), DAY(date)
        - Date macros: @between, @today, @dateEquals, @dateBefore, @dateAfter
        - String functions: CONCAT, SUBSTRING, UPPER, LOWER, TRIM
        - Aggregate functions: COUNT, SUM, AVG, MAX, MIN
        - Collection functions: SIZE(collection), IS EMPTY/NOT EMPTY
        - CASE WHEN expressions and COALESCE for null handling

        Without domain model tools first, queries may fail due to incorrect entity/attribute names.
        For date ranges, prefer Jmix macros over parameters for better handling.
        """)
    public QueryExecutionResult executeQuery(
            @ToolParam(description = "JPQL query with AS aliases for all SELECT fields") String jpqlQuery,
            @ToolParam(description = "Named parameters for the query (empty map if none)") Map<String, Object> parameters,
            @ToolParam(description = "List of aliases used in SELECT clause, in order (e.g., ['clientName', 'orderCount'])") List<String> selectAliases) {
        try {
            log.info("LLM Tool Call: executeQuery() - JPQL: {}", jpqlQuery);

            QueryExecutionResult result = aiJpqlQueryService.executeJpqlQuery(jpqlQuery, parameters, selectAliases);

            if (result.success()) {
                log.info("Query Result: {} rows returned", result.rowCount());
            } else {
                log.warn("Query Failed: {}", result.errorMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("Query Error: {} - {}", jpqlQuery, e.getMessage());
            return new QueryExecutionResult(
                    false,
                    List.of(),
                    0,
                    "Error executing query: " + e.getMessage()
            );
        }
    }


}