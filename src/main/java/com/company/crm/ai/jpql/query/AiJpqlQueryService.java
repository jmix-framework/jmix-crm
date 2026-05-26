package com.company.crm.ai.jpql.query;

import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetadataObject;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.EntityOp;
import io.jmix.data.QueryTransformerFactory;
import io.jmix.data.accesscontext.LoadValuesAccessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiJpqlQueryService {

    private static final Logger log = LoggerFactory.getLogger(AiJpqlQueryService.class);

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private final Metadata metadata;
    private final DataManager dataManager;
    private final AccessManager accessManager;
    private final ResultConverter resultConverter;
    private final AiJpqlParameterConverter parameterConverter;
    private final QueryTransformerFactory queryTransformerFactory;
    private final JpqlNamedParameterParser namedParameterParser;

    public AiJpqlQueryService(DataManager dataManager,
                              AiJpqlParameterConverter parameterConverter,
                              ResultConverter resultConverter,
                              AccessManager accessManager,
                              QueryTransformerFactory queryTransformerFactory,
                              Metadata metadata,
                              JpqlNamedParameterParser namedParameterParser) {
        this.dataManager = dataManager;
        this.parameterConverter = parameterConverter;
        this.resultConverter = resultConverter;
        this.accessManager = accessManager;
        this.queryTransformerFactory = queryTransformerFactory;
        this.metadata = metadata;
        this.namedParameterParser = namedParameterParser;
    }

    /**
     * Execute JPQL query with parameters for LLM-based queries
     *
     * @param jpqlQuery     The JPQL query to execute
     * @param parameters    Named JPQL parameters
     * @param selectAliases List of aliases for SELECT fields in order
     * @param offset        Starting row offset
     * @param limit         Maximum number of rows to return
     * @return Query execution result
     */
    public QueryExecutionResult executeJpqlQuery(String jpqlQuery, JpqlParameters parameters, List<String> selectAliases, Integer offset, Integer limit) {
        int effectiveOffset = (offset != null) ? Math.max(0, offset) : 0;
        int effectiveLimit = (limit != null) ? Math.clamp(limit, 1, MAX_LIMIT) : DEFAULT_LIMIT;
        ensureQueryIsPermitted(jpqlQuery);

        // First attempt: try with converted parameters
        QueryExecutionResult result = executeJpqlQueryWithParameters(jpqlQuery, parameters, selectAliases, effectiveOffset, effectiveLimit, true);

        if (!result.success()) {
            // Fallback: try with original parameters (no conversion)
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
    private QueryExecutionResult executeJpqlQueryWithParameters(String jpqlQuery, JpqlParameters parameters, List<String> selectAliases, int offset, int limit, boolean converted) {
        return executeJpqlQueryWithParameters(jpqlQuery, parameters, selectAliases, offset, limit, converted, true);
    }

    private QueryExecutionResult executeJpqlQueryWithParameters(String jpqlQuery,
                                                                JpqlParameters parameters,
                                                                List<String> selectAliases,
                                                                int offset,
                                                                int limit,
                                                                boolean converted,
                                                                boolean retryUnknownParameter) {
        try {
            long startTime = System.currentTimeMillis();
            var loadValuesBuilder = dataManager.loadValues(jpqlQuery);
            Set<String> queryParameterNames = namedParameterParser.extractNames(jpqlQuery);

            log.debug("JPQL Query: {}", jpqlQuery);
            log.debug("Extracted parameter names: {}", queryParameterNames);
            log.debug("Provided parameters: {}", parameters);

            // Convert JpqlParameters to Map for processing
            Map<String, Object> parameterMap = converted ?
                    parameterConverter.convertParameters(parameters.parameters()) :
                    convertJpqlParametersToMap(parameters);

            if (!parameterMap.isEmpty()) {
                for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
                    log.debug("Processing parameter '{}' with value '{}' (type: {})",
                             entry.getKey(), entry.getValue(),
                             entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
                    // Ignore extra parameters that are not referenced in the JPQL.
                    if (queryParameterNames.contains(entry.getKey())) {
                        log.debug("Setting parameter '{}' to value '{}'", entry.getKey(), entry.getValue());
                        loadValuesBuilder.parameter(entry.getKey(), entry.getValue());
                    } else {
                        log.debug("Ignoring parameter '{}' as it's not found in query parameter names", entry.getKey());
                    }
                }
            }

            String[] propertyNames = propertyNames(selectAliases);
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
            if (retryUnknownParameter) {
                String unknownParameter = namedParameterParser.extractUnknownParameterName(e.getMessage());
                if (unknownParameter != null && namedParameterParser.contains(parameters, unknownParameter)) {
                    JpqlParameters filteredParameters = namedParameterParser.without(parameters, unknownParameter);
                    log.debug("Retrying query without unknown parameter '{}'", unknownParameter);
                    return executeJpqlQueryWithParameters(jpqlQuery, filteredParameters, selectAliases, offset, limit, converted, false);
                }
            }
            return QueryExecutionResult.failed(e.getMessage());
        }
    }

    private Map<String, Object> convertJpqlParametersToMap(JpqlParameters parameters) {
        if (parameters == null || parameters.parameters().isEmpty()) {
            return Map.of();
        }
        return parameters.parameters().stream()
                .collect(java.util.stream.Collectors.toMap(
                        JpqlParameter::parameterName,
                        JpqlParameter::parameterValue
                ));
    }

    private String[] propertyNames(List<String> selectAliases) {
        return selectAliases != null ? selectAliases.toArray(new String[0]) : new String[0];
    }

    private void ensureQueryIsPermitted(String jpqlQuery) {
        LoadValuesAccessContext queryContext = new LoadValuesAccessContext(jpqlQuery, queryTransformerFactory, metadata);
        accessManager.applyRegisteredConstraints(queryContext);
        if (!queryContext.isPermitted()) {
            String entityNames = queryContext.getEntityClasses().stream()
                    .map(MetadataObject::getName)
                    .sorted()
                    .collect(Collectors.joining(","));
            String deniedResource = entityNames.isBlank() ? jpqlQuery : entityNames;
            throw new AccessDeniedException("entity", deniedResource, EntityOp.READ.getId());
        }
    }

}
