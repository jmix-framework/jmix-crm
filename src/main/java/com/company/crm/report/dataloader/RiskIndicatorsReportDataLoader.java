package com.company.crm.report.dataloader;

import com.company.crm.app.service.client.Client360ReportService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.RiskLevel;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.report.util.ReportDataLoaderUtils;
import io.jmix.core.DataManager;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DataLoader for Risk Indicators section of Client360Report.
 * Provides risk assessment including overdue invoices, payment duration and risk level classification.
 */
@Component("riskIndicatorsReportDataLoader")
public class RiskIndicatorsReportDataLoader implements ReportDataLoader {

    private final DataManager dataManager;
    private final Client360ReportService client360ReportService;

    public RiskIndicatorsReportDataLoader(DataManager dataManager, Client360ReportService client360ReportService) {
        this.dataManager = dataManager;
        this.client360ReportService = client360ReportService;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        LocalDateRange dateRange = ReportDataLoaderUtils.getDateRangeFromParams(params);
        UUID clientId = client.getId();

        // Overdue invoices count
        Long overdueCount = dataManager.loadValue("SELECT COUNT(i) FROM Invoice i " +
                "WHERE i.client.id = :clientId AND i.date BETWEEN :fromDate AND :toDate " +
                "AND i.status = :overdueStatus", Long.class)
                .parameter("clientId", clientId)
                .parameter("fromDate", dateRange.startDate())
                .parameter("toDate", dateRange.endDate())
                .parameter("overdueStatus", InvoiceStatus.OVERDUE.getId())
                .one();

        // Overdue amount
        BigDecimal overdueAmount = dataManager.loadValue("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i " +
                "WHERE i.client.id = :clientId AND i.date BETWEEN :fromDate AND :toDate " +
                "AND i.status = :overdueStatus", BigDecimal.class)
                .parameter("clientId", clientId)
                .parameter("fromDate", dateRange.startDate())
                .parameter("toDate", dateRange.endDate())
                .parameter("overdueStatus", InvoiceStatus.OVERDUE.getId())
                .one();

        // Average payment duration calculation and Risk level assessment from service
        double avgPaymentDuration = client360ReportService.calculateAveragePaymentDuration(client, dateRange);
        RiskLevel riskLevel = client360ReportService.calculateRiskLevel(client, dateRange);

        Map<String, Object> fields = new HashMap<>();
        fields.put("overdueCount", overdueCount != null ? overdueCount : 0L);
        fields.put("overdueAmount", PriceDataType.defaultFormat(overdueAmount != null ? overdueAmount : BigDecimal.ZERO));
        fields.put("avgPaymentDuration", avgPaymentDuration);
        fields.put("avgPaymentDurationFormatted", String.format("%.0f days", avgPaymentDuration));
        fields.put("riskLevel", riskLevel);
        fields.put("riskLevelClass", getRiskLevelCssClass(riskLevel));

        return List.of(fields);
    }

    private String getRiskLevelCssClass(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> "risk-high";
            case MEDIUM -> "risk-medium";
            default -> "risk-low";
        };
    }
}