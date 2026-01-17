package com.company.crm.ai.jmix.query;

import java.util.List;
import java.util.Map;


/**
 * Result object for JPQL query execution
 */
public record QueryExecutionResult(boolean success, List<Map<String, Object>> data, int rowCount, String errorMessage) {
}