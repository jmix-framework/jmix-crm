package com.company.crm.app.service.query;

import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("crm_JpqlQueryService")
public class JpqlQueryService {

    private static final Logger log = LoggerFactory.getLogger(JpqlQueryService.class);

    private final DataManager dataManager;

    public JpqlQueryService(DataManager dataManager) {
        this.dataManager = dataManager;
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
        try {
            // Use the fluent API approach as recommended in Jmix documentation
            var loadValuesBuilder = dataManager.loadValues(jpqlQuery);

            // Set parameters
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                loadValuesBuilder.parameter(entry.getKey(), entry.getValue());
            }

            // Use provided aliases as property names
            String[] propertyNames = selectAliases != null ? selectAliases.toArray(new String[0]) : new String[0];
            if (propertyNames.length > 0) {
                loadValuesBuilder.properties(propertyNames);
            }

            // Execute query
            List<KeyValueEntity> results = loadValuesBuilder.list();

            // Convert results to List of Maps
            List<Map<String, Object>> resultMaps = convertToMapList(results, propertyNames);

            return new QueryExecutionResult(
                    true,
                    resultMaps,
                    results.size(),
                    null,
                    jpqlQuery,
                    parameters
            );

        } catch (Exception e) {
            log.error("JPQL Error: {}", e.getMessage());
            return new QueryExecutionResult(
                    false,
                    new ArrayList<>(),
                    0,
                    e.getMessage(),
                    jpqlQuery,
                    parameters
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