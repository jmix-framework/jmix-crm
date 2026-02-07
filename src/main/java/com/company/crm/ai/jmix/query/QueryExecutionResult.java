package com.company.crm.ai.jmix.query;

import java.util.List;
import java.util.Map;


/**
 * Result object for JPQL query execution
 */
public record QueryExecutionResult(boolean success, List<Map<String, Object>> data, int rowCount, String errorMessage) {

    /**
     * Factory method for successful query results
     */
    public static QueryExecutionResult success(List<Map<String, Object>> data) {
        return new QueryExecutionResult(true, data, data.size(), null);
    }

    /**
     * Factory method for failed query results
     */
    public static QueryExecutionResult failed(String errorMessage) {
        return new QueryExecutionResult(false, List.of(), 0, errorMessage);
    }
}