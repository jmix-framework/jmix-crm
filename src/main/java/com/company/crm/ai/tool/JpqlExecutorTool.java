package com.company.crm.ai.tool;

import com.company.crm.ai.jpql.query.AiJpqlQueryService;
import com.company.crm.ai.jpql.query.JpqlParameters;
import com.company.crm.ai.jpql.query.QueryExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationContext;
import java.util.List;

/**
 * Spring AI Tool for executing JPQL queries against JPA databases in Jmix applications.
 *
 * <p>This component provides AI systems with the ability to execute JPQL queries and retrieve
 * data from the database. It serves as the primary data access interface for AI-powered
 * business intelligence and analytics features.
 */
public class JpqlExecutorTool implements CrmAiTool {

    private static final Logger log = LoggerFactory.getLogger(JpqlExecutorTool.class);

    private final AiJpqlQueryService aiJpqlQueryService;
    private final AiToolStatusPublisher toolStatusPublisher;

    public static JpqlExecutorTool create(ApplicationContext applicationContext) {
        return new JpqlExecutorTool(
                applicationContext.getBean(AiJpqlQueryService.class),
                applicationContext.getBean(AiToolStatusPublisher.class));
    }

    public JpqlExecutorTool(AiJpqlQueryService aiJpqlQueryService, AiToolStatusPublisher toolStatusPublisher) {
        this.aiJpqlQueryService = aiJpqlQueryService;
        this.toolStatusPublisher = toolStatusPublisher;
    }

    /**
     * Execute a JPQL query with parameters
     *
     * @param jpqlQuery     The JPQL query to execute against the CRM database. Use proper entity names like 'Client', 'Order_', 'OrderItem', etc.
     * @param parameters    Named parameters for the query.
     * @param selectAliases List of aliases used in SELECT clause, in order.
     * @param offset        Optional: Starting row index (default: 0).
     * @param limit         Optional: Maximum number of rows to return (default: 50, max: 200).
     * @return Query execution result with data or error message
     */
    @Tool(description = """
            Execute JPQL queries against the JPA database to retrieve insights.
            
            CRITICAL REQUIREMENTS:
            1. MUST call domain model tools first to get complete entity schema information
            2. MUST use AS aliases for ALL SELECT fields (security requirement)
            3. MUST provide the selectAliases parameter with all aliases in order
            4. Use exact entity names and attribute names from the schema
            5. Prefer tested and reliable functions over advanced/experimental features
            
            PAGINATION:
            - Results are limited to 50 rows by default (max 200).
            - If the result contains 'hasMore: true', more data is available in the database.
            - To fetch the next set of results, call this tool again with an increased 'offset'.
            - Use server-side aggregation (COUNT, SUM, AVG) whenever possible instead of fetching all rows.
            
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
            - AS aliases are MANDATORY for all SELECT fields (security and parsing requirement)
            - No subqueries in SELECT clause
            - Use GROUP BY entity not GROUP BY entity.id
            
            IMPORTANT - AVOID JPQL RESERVED WORDS AS ALIASES:
            Never use these words as AS aliases: position, user, order, table, group, where, select, from, join,
            left, right, inner, outer, on, and, or, not, in, exists, between, like, is, null, true, false,
            count, sum, avg, max, min, distinct, all, any, some, union, except, intersect, case, when, then,
            else, end, new, constructor, size, index, key, value, entry, type, treat, current_date, current_time,
            current_timestamp, local, date, time, timestamp, year, month, day, hour, minute, second.
            
            ✓ CORRECT: SELECT co.position AS jobPosition (not AS position)
            ✗ INCORRECT: SELECT co.position AS position
            
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
            
            ENUM PARAMETERS (CRITICAL):
            - For enum properties, use id from domain model enums mapping (enums.<ENUM_NAME>.id).
            - Do NOT pass enum constant names when enums provides numeric/string IDs.
            - Example for Invoice.status: enums {NEW: {id: 10}, PENDING: {id: 20}, OVERDUE: {id: 30}, PAID: {id: 40}}
              ✓ CORRECT: WHERE i.status = :status with parameters {"status": 40}
              ✗ INCORRECT: WHERE i.status = :status with parameters {"status": "PAID"}
            - For IN filters, also pass mapped values list (e.g., {"statuses": [20, 30]}).
            
            Examples:
            ✓ Parameters: {"startDate": "2024-01-15", "minValue": "1000.00", "pattern": "%Test%"}
            ✓ Macros: @between(o.date, now-30, now, day)
            
            JMIX JPQL EXTENSIONS AND FUNCTIONS:
            
            DATE/TIME FUNCTIONS:
            - EXTRACT(field FROM date) - Extract date/time parts: YEAR, MONTH, DAY, HOUR, MINUTE, SECOND
              Examples: EXTRACT(YEAR FROM o.date), EXTRACT(MONTH FROM o.date)
            - CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP - Current date/time values
            - DATE(datetime) - Extract date part from datetime
            - TIME(datetime) - Extract time part from datetime
            
            MATHEMATICAL FUNCTIONS:
            - Basic arithmetic: +, -, *, / (e.g., o.total * 2, o.total + 1000)
            - Parentheses for operation precedence
            - ABS(number) - Absolute value (limited support)
            - ROUND(number, digits) - Round to specified decimal places (limited support)
            
            STRING FUNCTIONS:
            - CONCAT(str1, str2, ...) - Concatenate strings
            - SUBSTRING(string, start, length) - Extract substring
            - LENGTH(string) - String length
            - LOCATE(substring, string, start) - Find substring position
            - UPPER(string) - Convert to uppercase
            - LOWER(string) - Convert to lowercase
            - TRIM(string) - Remove leading/trailing whitespace
            - LIKE with wildcards (%, _) - Pattern matching (recommended)
            
            CONDITIONAL FUNCTIONS:
            - CASE WHEN condition THEN result ELSE alternative END - Conditional expressions
            - COALESCE(value1, value2, ...) - Return first non-null value
            - NULLIF(value1, value2) - Return null if values are equal
            
            AGGREGATE FUNCTIONS:
            - COUNT(entity) - Count entities (use entity, not *)
            - SUM(expression) - Sum of values
            - AVG(expression) - Average value
            - MAX(expression) - Maximum value
            - MIN(expression) - Minimum value
            - DISTINCT - Use with aggregates for unique values
            
            COLLECTION FUNCTIONS:
            - SIZE(collection) - Collection size
            - IS EMPTY / IS NOT EMPTY - Check if collection is empty
            
            DATE MACROS (Jmix-specific):
            - @between(field, start, end, unit) - Date range queries
              Units: year, month, day, hour, minute, second
              Examples: @between(o.date, now-30, now, day), @between(o.date, now-1, now, month)
            - @today(field) - Today's date
            - @dateEquals(field, value) - Date equality
            - @dateBefore(field, value) - Date before
            - @dateAfter(field, value) - Date after
            - Special values: now, now-N (N units ago)
            
            BEST PRACTICES (TESTED AND RELIABLE):
            - Use EXTRACT for date parts instead of proprietary functions
            - Use LIKE with wildcards instead of REGEXP for pattern matching
            - Use basic arithmetic (+, -, *, /) instead of advanced mathematical functions
            - Use Jmix date macros (@between, @today) for date filtering
            - Test complex functions in development before using in production queries
            
            CRITICAL JPQL PARSER LIMITATIONS:
            ✗ INCORRECT: COUNT(CASE WHEN o.date >= '2025-01-01' THEN 1 END)
            ✗ INCORRECT: SUM(CASE WHEN @between(o.date, now-90, now, day) THEN o.total ELSE 0 END)
            
            ✓ CORRECT: Use separate queries with simple WHERE clauses for period comparisons.
            
            Without domain model tools first, queries may fail due to incorrect entity/attribute names.
            For date ranges, prefer Jmix macros over parameters for better handling.
            """)
    public QueryExecutionResult executeQuery(
            @ToolParam(description = "JPQL query with AS aliases for all SELECT fields") String jpqlQuery,
            @ToolParam(description = "Query parameters. Provide named parameters for :parameterName placeholders in the JPQL query. Leave empty if no parameters needed.") JpqlParameters parameters,
            @ToolParam(description = "List of aliases used in SELECT clause, in order (e.g., ['clientName', 'orderCount'])") List<String> selectAliases,
            @ToolParam(description = "Optional: Starting row index (default: 0)") Integer offset,
            @ToolParam(description = "Optional: Maximum number of rows to return (default: 50, max: 200)") Integer limit,
            ToolContext toolContext) {
        String statusStart = "Querying CRM data...";
        try {
            log.info("LLM Tool Call: executeQuery(offset={}, limit={})", offset, limit);
            toolStatusPublisher.update(toolContext, statusStart);

            QueryExecutionResult result = aiJpqlQueryService.executeJpqlQuery(jpqlQuery, parameters, selectAliases, offset, limit);
            String snippet = String.format("Query successful (%d rows found)", result.rowCount());
            toolStatusPublisher.complete(toolContext, statusStart, snippet);
            return result;
        } catch (Exception e) {
            log.error("Query Error: {} - {}", jpqlQuery, e.getMessage());
            QueryExecutionResult failed = QueryExecutionResult.failed("Error executing query: " + e.getMessage());
            String snippet = String.format("Query failed: %s", e.getMessage());
            toolStatusPublisher.complete(toolContext, statusStart, snippet);
            return failed;
        }
    }
}
