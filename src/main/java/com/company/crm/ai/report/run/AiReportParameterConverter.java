package com.company.crm.ai.report.run;

import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportInputParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart converter for report parameters that converts LLM-provided values into Jmix-compatible types.
 */
@Component
public class AiReportParameterConverter {

    private static final Logger log = LoggerFactory.getLogger(AiReportParameterConverter.class);

    private final DataManager dataManager;
    private final Metadata metadata;
    private final MetadataTools metadataTools;
    private final ConversionService conversionService;

    public AiReportParameterConverter(DataManager dataManager,
                                     Metadata metadata,
                                     MetadataTools metadataTools,
                                     ConversionService conversionService) {
        this.dataManager = dataManager;
        this.metadata = metadata;
        this.metadataTools = metadataTools;
        this.conversionService = conversionService;
    }

    /**
     * Converts a map of raw LLM parameters into typed Jmix objects based on report parameter definitions.
     *
     * @param reportParameters List of report parameter definitions
     * @param inputParameters  Map of parameter aliases to values provided by LLM
     * @return Result object containing converted parameters or validation errors
     */
    public ReportParameterConversionResult convertParameters(List<ReportInputParameter> reportParameters,
                                                            Map<String, Object> inputParameters) {
        if (inputParameters == null) {
            inputParameters = Collections.emptyMap();
        }

        Map<String, Object> convertedValues = new HashMap<>();
        List<ReportValidationError> errors = new ArrayList<>();
        boolean hasConversionErrors = false;

        Map<String, ReportInputParameter> parameterMap = reportParameters.stream()
                .collect(Collectors.toMap(ReportInputParameter::getAlias, p -> p));

        for (Map.Entry<String, Object> entry : inputParameters.entrySet()) {
            String alias = entry.getKey();
            Object rawValue = entry.getValue();

            ReportInputParameter paramDef = parameterMap.get(alias);
            if (paramDef == null) {
                log.debug("Unknown report parameter alias: {}", alias);
                errors.add(new ReportValidationError(alias, "Unknown report parameter alias."));
                continue;
            }

            try {
                Object convertedValue = convertValue(paramDef, rawValue);
                if (convertedValue != null) {
                    convertedValues.put(alias, convertedValue);
                }
            } catch (Exception e) {
                log.debug("Failed to convert parameter {}: {}", alias, e.getMessage());
                errors.add(new ReportValidationError(alias, "Conversion failed: " + e.getMessage()));
                hasConversionErrors = true;
            }
        }

        // Check for missing required parameters
        for (ReportInputParameter paramDef : reportParameters) {
            if (Boolean.TRUE.equals(paramDef.getRequired()) && !convertedValues.containsKey(paramDef.getAlias())) {
                errors.add(new ReportValidationError(paramDef.getAlias(), "Required parameter is missing"));
            }
        }

        if (!errors.isEmpty()) {
            return ReportParameterConversionResult.failed(errors, hasConversionErrors);
        }

        return ReportParameterConversionResult.success(convertedValues);
    }

    private Object convertValue(ReportInputParameter paramDef, Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        ParameterType type = paramDef.getType();
        if (type == null) {
            return rawValue;
        }

        return switch (type) {
            case TEXT -> String.valueOf(rawValue);
            case NUMERIC -> conversionService.convert(rawValue, BigDecimal.class);
            case BOOLEAN -> conversionService.convert(rawValue, Boolean.class);
            case DATE -> convertToDate(rawValue);
            case TIME -> convertToTime(rawValue);
            case DATETIME -> convertToDateTime(rawValue);
            case ENTITY -> convertToEntity(paramDef.getEntityMetaClass(), rawValue);
            case ENTITY_LIST -> convertToEntityList(paramDef.getEntityMetaClass(), rawValue);
            case ENUMERATION -> convertToEnum(paramDef.getEnumerationClass(), rawValue);
            default -> rawValue;
        };
    }

    private Date convertToDate(Object value) {
        LocalDate localDate = conversionService.convert(value, LocalDate.class);
        return localDate != null ? Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
    }

    private Date convertToTime(Object value) {
        LocalTime localTime = conversionService.convert(value, LocalTime.class);
        return localTime != null ? Date.from(localTime.atDate(LocalDate.of(1970, 1, 1)).atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    private Date convertToDateTime(Object value) {
        LocalDateTime localDateTime = conversionService.convert(value, LocalDateTime.class);
        return localDateTime != null ? Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    private Object convertToEntity(String entityMetaClass, Object value) {
        if (entityMetaClass == null || value == null) {
            return null;
        }
        String idString = String.valueOf(value);
        MetaClass metaClass = metadata.getClass(entityMetaClass);
        MetaProperty primaryKeyProperty = metadataTools.getPrimaryKeyProperty(metaClass);
        if (primaryKeyProperty == null) {
            throw new RuntimeException("Primary key property not found for " + entityMetaClass);
        }
        Object id = conversionService.convert(idString, primaryKeyProperty.getJavaType());
        return dataManager.load(Id.of(id, metaClass.getJavaClass())).one();
    }

    private List<Object> convertToEntityList(String entityMetaClass, Object value) {
        if (entityMetaClass == null) {
            return null;
        }
        if (!(value instanceof Collection<?> collection)) {
            throw new RuntimeException("Value must be a collection for ENTITY_LIST parameter");
        }
        MetaClass metaClass = metadata.getClass(entityMetaClass);
        MetaProperty primaryKeyProperty = metadataTools.getPrimaryKeyProperty(metaClass);
        if (primaryKeyProperty == null) {
            throw new RuntimeException("Primary key property not found for " + entityMetaClass);
        }
        Class<?> idType = primaryKeyProperty.getJavaType();
        Class<?> javaClass = metaClass.getJavaClass();

        List<Object> ids = collection.stream()
                .map(v -> conversionService.convert(String.valueOf(v), idType))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ids.stream()
                .map(id -> dataManager.load(Id.of(id, javaClass)).one())
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertToEnum(String enumerationClass, Object value) {
        if (enumerationClass == null || value == null) {
            return null;
        }
        try {
            Class<Enum> enumClass = (Class<Enum>) Class.forName(enumerationClass);
            return Enum.valueOf(enumClass, String.valueOf(value));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to enum " + enumerationClass, e);
        }
    }
}
