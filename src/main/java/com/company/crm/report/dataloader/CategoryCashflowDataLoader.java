package com.company.crm.report.dataloader;

import com.company.crm.app.service.analytics.CashflowAnalyticsService;
import com.company.crm.app.service.analytics.CategoryCashflowRiskAssessmentResult;
import com.company.crm.app.service.analytics.CategoryRiskMetrics;
import com.company.crm.model.client.Client;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * DataLoader for Category Cashflow Risk report.
 * Provides data set for RiskByCategory.
 */
@Component("crm_CategoryCashflowDataLoader")
public class CategoryCashflowDataLoader implements ReportDataLoader {

    private static final String DATASET_HEADER = "header";
    private static final String CACHE_KEY = "__categoryCashflowRiskAssessment";

    private final CashflowAnalyticsService cashflowAnalyticsService;

    public CategoryCashflowDataLoader(CashflowAnalyticsService cashflowAnalyticsService) {
        this.cashflowAnalyticsService = cashflowAnalyticsService;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        if (reportQuery != null && DATASET_HEADER.equals(reportQuery.getName())) {
            return List.of(Collections.emptyMap());
        }

        CategoryCashflowRiskAssessmentResult assessment = getOrComputeAssessment(params);
        return assessment.riskByCategory().stream()
                .map(this::toRiskByCategoryMap)
                .toList();
    }

    private CategoryCashflowRiskAssessmentResult getOrComputeAssessment(Map<String, Object> params) {
        Object cached = params.get(CACHE_KEY);
        if (cached instanceof CategoryCashflowRiskAssessmentResult result) {
            return result;
        }

        LocalDate fromDate = convertToLocalDate(params.get("fromDate"));
        LocalDate toDate = convertToLocalDate(params.get("toDate"));
        LocalDate asOfDate = convertToLocalDate(params.get("asOfDate"));
        Boolean includePaid = convertToBoolean(params.get("includePaid"));
        UUID clientId = convertToClientId(params.get("client"));

        CategoryCashflowRiskAssessmentResult result = cashflowAnalyticsService
                .assessCategoryCashflowRiskReport(fromDate, toDate, clientId, includePaid, asOfDate);
        params.put(CACHE_KEY, result);
        return result;
    }

    private Map<String, Object> toRiskByCategoryMap(CategoryRiskMetrics metric) {
        Map<String, Object> map = new HashMap<>();
        map.put("categoryCode", metric.categoryCode());
        map.put("categoryName", metric.categoryName());
        map.put("invoicedAmount", metric.invoicedAmount());
        map.put("paidAmount", metric.paidAmount());
        map.put("openAmount", metric.openAmount());
        map.put("overdueOpenAmount", metric.overdueOpenAmount());
        map.put("dtcDaysWeighted", metric.dtcDaysWeighted());
        map.put("paymentsCount", metric.paymentsCount());
        map.put("invoicesCount", metric.invoicesCount());
        map.put("overpaymentAmount", metric.overpaymentAmount());
        return map;
    }

    private UUID convertToClientId(Object clientParam) {
        if (clientParam instanceof Client client) {
            return client.getId();
        }
        if (clientParam instanceof UUID id) {
            return id;
        }
        if (clientParam instanceof String idAsString) {
            try {
                return UUID.fromString(idAsString);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return null;
    }

    private LocalDate convertToLocalDate(Object date) {
        if (date instanceof LocalDate localDate) return localDate;
        if (date instanceof java.util.Date d) return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return null;
    }
}
