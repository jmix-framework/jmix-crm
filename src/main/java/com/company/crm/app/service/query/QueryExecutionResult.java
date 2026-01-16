package com.company.crm.app.service.query;

import java.util.List;
import java.util.Map;


/**
 * Result object for JPQL query execution
 */
public record QueryExecutionResult(boolean success, List<Map<String, Object>> data, int rowCount, String errorMessage,
                                   String executedQuery, Map<String, Object> parameters) {
}