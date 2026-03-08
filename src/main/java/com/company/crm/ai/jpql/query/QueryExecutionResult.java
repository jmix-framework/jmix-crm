package com.company.crm.ai.jpql.query;

import java.util.List;
import java.util.Map;


/**
 * Result object for JPQL query execution
 */
public record QueryExecutionResult(
        boolean success,
        List<Map<String, Object>> data,
        int rowCount,
        boolean hasMore,
        int offset,
        int limit,
        String errorMessage
) {

    /**
     * Factory method for successful query results
     */
    public static QueryExecutionResult success(List<Map<String, Object>> data, boolean hasMore, int offset, int limit) {
        return new QueryExecutionResult(true, data, data.size(), hasMore, offset, limit, null);
    }

    /**
     * Factory method for failed query results
     */
    public static QueryExecutionResult failed(String errorMessage) {
        return new QueryExecutionResult(false, List.of(), 0, false, 0, 0, errorMessage);
    }
}