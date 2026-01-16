package com.company.crm.app.service.ai;

import java.util.Map;

/**
 * Result object for CRM Query Tool responses with JSON serialization
 * This is what gets returned to the LLM from the Tool
 */
public record CrmQueryResult(boolean success, String data, int rowCount, String errorMessage,
                            String executedQuery, Map<String, Object> parameters) {
}