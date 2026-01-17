package com.company.crm.ai.jmix.query;

import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("ai_AiJpqlQueryService")
public class AiJpqlQueryService {

    private static final Logger log = LoggerFactory.getLogger(AiJpqlQueryService.class);

    private final DataManager dataManager;
    private final AiJpqlParameterConverter parameterConverter;

    public AiJpqlQueryService(DataManager dataManager, AiJpqlParameterConverter parameterConverter) {
        this.dataManager = dataManager;
        this.parameterConverter = parameterConverter;
    }


    /**
     * Execute JPQL query with parameters for LLM-based queries
     *
     * @param jpqlQuery The JPQL query to execute
     * @param parameters Named parameters as key-value map
     * @param selectAliases List of aliases for SELECT fields in order
     * @return Query execution result
     */
    public QueryExecutionResult executeJpqlQuery(String jpqlQuery, Map<String, Object> parameters, List<String> selectAliases) {
        // First attempt: try with converted parameters
        QueryExecutionResult result = executeJpqlQueryWithParameters(jpqlQuery, parameterConverter.convertParameters(parameters), selectAliases, true);

        if (!result.success()) {
            // Fallback: try with original parameters
            log.info("Query failed with converted parameters, trying with original parameters. Error: {}", result.errorMessage());
            QueryExecutionResult fallbackResult = executeJpqlQueryWithParameters(jpqlQuery, parameters, selectAliases, false);

            if (fallbackResult.success()) {
                log.info("Query succeeded with original parameters after conversion failed");
            }
            return fallbackResult;
        }

        return result;
    }

    /**
     * Internal method to execute JPQL query with given parameters
     */
    private QueryExecutionResult executeJpqlQueryWithParameters(String jpqlQuery, Map<String, Object> parameters, List<String> selectAliases, boolean converted) {
        try {
            var loadValuesBuilder = dataManager.loadValues(jpqlQuery);

            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    loadValuesBuilder.parameter(entry.getKey(), entry.getValue());
                }
            }

            String[] propertyNames = selectAliases != null ? selectAliases.toArray(new String[0]) : new String[0];
            if (propertyNames.length > 0) {
                loadValuesBuilder.properties(propertyNames);
            }

            List<KeyValueEntity> results = loadValuesBuilder.list();

            List<Map<String, Object>> resultMaps = convertToMapList(results, propertyNames);

            log.debug("Query executed successfully with {} parameters, {} rows returned",
                    converted ? "converted" : "original", results.size());

            return new QueryExecutionResult(
                    true,
                    resultMaps,
                    results.size(),
                    null
            );

        } catch (Exception e) {
            log.debug("Query failed with {} parameters: {}", converted ? "converted" : "original", e.getMessage());
            return new QueryExecutionResult(
                    false,
                    new ArrayList<>(),
                    0,
                    e.getMessage()
            );
        }
    }

    /**
     * Convert KeyValueEntity results to List of Maps
     */
    private List<Map<String, Object>> convertToMapList(List<KeyValueEntity> results, String[] propertyNames) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> resultList = new ArrayList<>();

            for (KeyValueEntity entity : results) {
                Map<String, Object> rowMap = new HashMap<>();

                for (String propName : propertyNames) {
                    Object value = entity.getValue(propName);
                    rowMap.put(propName, value);
                }

                resultList.add(rowMap);
            }

            return resultList;

        } catch (Exception e) {
            log.error("Error converting KeyValueEntities to Maps: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}