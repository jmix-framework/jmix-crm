package com.company.crm.ai.jmix.query;

import io.jmix.core.DataManager;
import io.jmix.core.AccessManager;
import io.jmix.core.Metadata;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.EntityOp;
import io.jmix.data.QueryTransformerFactory;
import io.jmix.data.accesscontext.LoadValuesAccessContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("ai_AiJpqlQueryService")
public class AiJpqlQueryService {

    private static final Logger log = LoggerFactory.getLogger(AiJpqlQueryService.class);

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private final DataManager dataManager;
    private final AiJpqlParameterConverter parameterConverter;
    private final ResultConverter resultConverter;
    private final AccessManager accessManager;
    private final QueryTransformerFactory queryTransformerFactory;
    private final Metadata metadata;

    public AiJpqlQueryService(DataManager dataManager,
                             AiJpqlParameterConverter parameterConverter,
                             ResultConverter resultConverter,
                             AccessManager accessManager,
                             QueryTransformerFactory queryTransformerFactory,
                             Metadata metadata) {
        this.dataManager = dataManager;
        this.parameterConverter = parameterConverter;
        this.resultConverter = resultConverter;
        this.accessManager = accessManager;
        this.queryTransformerFactory = queryTransformerFactory;
        this.metadata = metadata;
    }


    /**
     * Execute JPQL query with parameters for LLM-based queries
     *
     * @param jpqlQuery The JPQL query to execute
     * @param parameters Named parameters as key-value map
     * @param selectAliases List of aliases for SELECT fields in order
     * @param offset Starting row offset
     * @param limit Maximum number of rows to return
     * @return Query execution result
     */
    public QueryExecutionResult executeJpqlQuery(String jpqlQuery, Map<String, Object> parameters, List<String> selectAliases, Integer offset, Integer limit) {
        int effectiveOffset = (offset != null) ? Math.max(0, offset) : 0;
        int effectiveLimit = (limit != null) ? Math.min(MAX_LIMIT, Math.max(1, limit)) : DEFAULT_LIMIT;
        ensureQueryIsPermitted(jpqlQuery);

        // First attempt: try with converted parameters
        QueryExecutionResult result = executeJpqlQueryWithParameters(jpqlQuery, parameterConverter.convertParameters(parameters), selectAliases, effectiveOffset, effectiveLimit, true);

        if (!result.success()) {
            // Fallback: try with original parameters
            log.info("Query failed with converted parameters, trying with original parameters. Error: {}", result.errorMessage());
            QueryExecutionResult fallbackResult = executeJpqlQueryWithParameters(jpqlQuery, parameters, selectAliases, effectiveOffset, effectiveLimit, false);

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
    private QueryExecutionResult executeJpqlQueryWithParameters(String jpqlQuery, Map<String, Object> parameters, List<String> selectAliases, int offset, int limit, boolean converted) {
        try {
            long startTime = System.currentTimeMillis();
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

            // Fetch limit + 1 to detect if more rows exist
            loadValuesBuilder.firstResult(offset);
            loadValuesBuilder.maxResults(limit + 1);

            List<KeyValueEntity> results = loadValuesBuilder.list();
            long duration = System.currentTimeMillis() - startTime;

            boolean hasMore = results.size() > limit;
            List<KeyValueEntity> finalResults = hasMore ? results.subList(0, limit) : results;

            List<Map<String, Object>> resultMaps = resultConverter.convertToMapList(finalResults, propertyNames);

            log.debug("Query executed successfully with {} parameters. Rows: {}, hasMore: {}, duration: {}ms",
                    converted ? "converted" : "original", resultMaps.size(), hasMore, duration);

            return QueryExecutionResult.success(resultMaps, hasMore, offset, limit);

        } catch (Exception e) {
            log.debug("Query failed with {} parameters: {}", converted ? "converted" : "original", e.getMessage());
            return QueryExecutionResult.failed(e.getMessage());
        }
    }

    private void ensureQueryIsPermitted(String jpqlQuery) {
        LoadValuesAccessContext queryContext = new LoadValuesAccessContext(jpqlQuery, queryTransformerFactory, metadata);
        accessManager.applyRegisteredConstraints(queryContext);
        if (!queryContext.isPermitted()) {
            String entityNames = queryContext.getEntityClasses().stream()
                    .map(metaClass -> metaClass.getName())
                    .sorted()
                    .collect(Collectors.joining(","));
            String deniedResource = entityNames.isBlank() ? jpqlQuery : entityNames;
            throw new AccessDeniedException("entity", deniedResource, EntityOp.READ.getId());
        }
    }

}
