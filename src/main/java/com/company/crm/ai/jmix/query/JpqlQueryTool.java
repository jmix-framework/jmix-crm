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
        2. MUST use AS aliases for ALL SELECT fields (security requirement)
        3. MUST provide the selectAliases parameter with all aliases in order
        4. Use exact entity names and attribute names from the schema
        5. Prefer tested and reliable functions over advanced/experimental features

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

        Examples:
        ✓ Parameters: {"startDate": "2024-01-15", "minValue": "1000.00", "pattern": "%Test%"}
        ✓ Macros: @between(o.date, now-30, now, day)

        JMIX JPQL EXTENSIONS AND FUNCTIONS:

        DATE/TIME FUNCTIONS:
        - EXTRACT(field FROM date) - Extract date/time parts: YEAR, MONTH, DAY, HOUR, MINUTE, SECOND
          Examples: EXTRACT(YEAR FROM o.date), EXTRACT(MONTH FROM o.createdDate)
        - CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP - Current date/time values
        - DATE(datetime) - Extract date part from datetime
        - TIME(datetime) - Extract time part from datetime

        MATHEMATICAL FUNCTIONS:
        - Basic arithmetic: +, -, *, / (e.g., o.total * 2, o.total + 1000)
        - Parentheses for operation precedence
        - ABS(number) - Absolute value (limited support)
        - ROUND(number, digits) - Round to specified decimal places (limited support)

        Note: Advanced mathematical functions (SQRT, MOD, CEIL, FLOOR, POWER, EXP, LN, LOG)
        may not be fully supported in all EclipseLink versions. Use basic arithmetic where possible.

        STRING FUNCTIONS:
        - CONCAT(str1, str2, ...) - Concatenate strings
        - SUBSTRING(string, start, length) - Extract substring
        - LENGTH(string) - String length
        - LOCATE(substring, string, start) - Find substring position
        - UPPER(string) - Convert to uppercase
        - LOWER(string) - Convert to lowercase
        - TRIM(string) - Remove leading/trailing whitespace
        - TRIM(LEADING/TRAILING/BOTH char FROM string) - Trim specific characters
        - LEFT(string, length) - Left substring (limited support)
        - RIGHT(string, length) - Right substring (limited support)
        - REPLACE(string, search, replace) - Replace occurrences (limited support)
        - LIKE with wildcards (%, _) - Pattern matching (recommended)
        - REGEXP(string, pattern) - Regular expression matching (limited support, use LIKE instead)

        CONDITIONAL FUNCTIONS:
        - CASE WHEN condition THEN result ELSE alternative END - Conditional expressions
        - COALESCE(value1, value2, ...) - Return first non-null value
        - NULLIF(value1, value2) - Return null if values are equal

        TYPE CONVERSION:
        - CAST(expression AS type) - Type conversion (limited support)
          Types: text, int, long, double, boolean, date, time, timestamp
          Example: CAST(e.number AS text), CAST(:param AS double)
          Note: Basic type conversions may work, but prefer native column types when possible

        AGGREGATE FUNCTIONS:
        - COUNT(entity) - Count entities (use entity, not *)
        - SUM(expression) - Sum of values (returns Number, may be Double)
        - AVG(expression) - Average value (returns Number, may be Double)
        - MAX(expression) - Maximum value
        - MIN(expression) - Minimum value
        - DISTINCT - Use with aggregates for unique values

        Note: Aggregate functions may return Double for calculated values instead of BigDecimal

        COLLECTION FUNCTIONS:
        - SIZE(collection) - Collection size
        - IS EMPTY / IS NOT EMPTY - Check if collection is empty
        - MEMBER OF - Check collection membership
        - INDEX(alias) - Index of element in ordered collection

        DATE MACROS (Jmix-specific):
        - @between(field, start, end, unit) - Date range queries
          Units: year, month, day, hour, minute, second
          Examples: @between(o.date, now-30, now, day), @between(o.date, now-1, now, month)
        - @today(field) - Today's date
        - @dateEquals(field, value) - Date equality
        - @dateBefore(field, value) - Date before
        - @dateAfter(field, value) - Date after
        - Special values: now, now-N (N units ago)

        EXAMPLES:
        - Monthly revenue: SELECT EXTRACT(MONTH FROM o.date) AS month, SUM(o.total) AS revenue FROM Order_ o GROUP BY EXTRACT(MONTH FROM o.date)
        - Client search: SELECT c FROM Client c WHERE UPPER(c.name) LIKE UPPER(:pattern)
        - Pattern matching: SELECT c FROM Client c WHERE UPPER(c.name) LIKE '%CORP%' OR UPPER(c.name) LIKE '%ENTERPRISE%'
        - Recent orders: SELECT o FROM Order_ o WHERE @between(o.date, now-30, now, day)
        - Order statistics: SELECT COUNT(o) AS orderCount, AVG(o.total) AS avgValue FROM Order_ o
        - Basic math: SELECT o.total AS originalTotal, (o.total * 1.19) AS totalWithTax FROM Order_ o
        - Conditional logic: SELECT c.name AS clientName, CASE WHEN COUNT(o) > 2 THEN 'High Volume' ELSE 'Regular' END AS category FROM Client c LEFT JOIN c.orders o GROUP BY c

        BEST PRACTICES (TESTED AND RELIABLE):
        - Use EXTRACT for date parts instead of proprietary functions
        - Use LIKE with wildcards instead of REGEXP for pattern matching
        - Use basic arithmetic (+, -, *, /) instead of advanced mathematical functions
        - Prefer native column types over CAST conversions
        - Use Jmix date macros (@between, @today) for date filtering
        - Be aware that aggregate functions may return Double instead of BigDecimal
        - Test complex functions in development before using in production queries

        AVOID THESE (UNRELIABLE OR LIMITED SUPPORT):
        - Advanced math functions: SQRT, MOD, CEIL, FLOOR, POWER, EXP, LN, LOG
        - REGEXP for pattern matching (use LIKE instead)
        - Complex CAST operations
        - LEFT/RIGHT string functions (limited support)
        - CASE WHEN expressions inside aggregate functions (COUNT, SUM, etc.)
        - @between macros inside CASE WHEN expressions

        CRITICAL JPQL PARSER LIMITATIONS:
        ✗ INCORRECT: COUNT(CASE WHEN o.date >= '2025-01-01' THEN 1 END)
        ✗ INCORRECT: SUM(CASE WHEN @between(o.date, now-90, now, day) THEN o.total ELSE 0 END)
        ✗ INCORRECT: COUNT(CASE WHEN @between(o.date, now-90, now, day) THEN o ELSE NULL END)

        ✓ CORRECT: Use separate queries with simple WHERE clauses:
        - Query 1: WHERE o.date >= '2025-01-01' - then COUNT(o), SUM(o.total)
        - Query 2: WHERE @between(o.date, now-90, now, day) - then COUNT(o), SUM(o.total)
        - Query 3: WHERE @between(o.date, now-180, now-91, day) - then COUNT(o), SUM(o.total)

        For period comparisons, ALWAYS use multiple simple queries instead of CASE expressions.
        The application can then combine results from multiple queries to create comparisons.

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
                log.debug("Query Data returned to LLM: {}", result.data());
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