package com.company.crm.report.dataloader;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.report.util.ReportDataLoaderUtils;
import io.jmix.core.DataManager;
import io.jmix.core.MetadataTools;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DataLoader for Invoice Status Breakdown section of Client360Report.
 * Aggregates invoice counts and amounts by status for a client within the specified date range.
 */
@Component("invoiceStatusBreakdownReportDataLoader")
public class InvoiceStatusBreakdownReportDataLoader implements ReportDataLoader {

    private final DataManager dataManager;
    private final MetadataTools metadataTools;

    public InvoiceStatusBreakdownReportDataLoader(DataManager dataManager, MetadataTools metadataTools) {
        this.dataManager = dataManager;
        this.metadataTools = metadataTools;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        LocalDateRange dateRange = ReportDataLoaderUtils.getDateRangeFromParams(params);
        UUID clientId = client.getId();

        List<Map<String, Object>> breakdown = new ArrayList<>();

        for (InvoiceStatus status : InvoiceStatus.values()) {
            Long count = dataManager.loadValue("SELECT COUNT(i) FROM Invoice i " +
                    "WHERE i.client.id = :clientId AND i.date BETWEEN :fromDate AND :toDate " +
                    "AND i.status = :status", Long.class)
                    .parameter("clientId", clientId)
                    .parameter("fromDate", dateRange.startDate())
                    .parameter("toDate", dateRange.endDate())
                    .parameter("status", status.getId())
                    .one();

            BigDecimal amount = dataManager.loadValue("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i " +
                    "WHERE i.client.id = :clientId AND i.date BETWEEN :fromDate AND :toDate " +
                    "AND i.status = :status", BigDecimal.class)
                    .parameter("clientId", clientId)
                    .parameter("fromDate", dateRange.startDate())
                    .parameter("toDate", dateRange.endDate())
                    .parameter("status", status.getId())
                    .one();

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("status", status.name());
            statusData.put("statusFormatted", metadataTools.format(status));
            statusData.put("count", count != null ? count : 0L);
            statusData.put("amount", PriceDataType.defaultFormat(amount != null ? amount : BigDecimal.ZERO));
            breakdown.add(statusData);
        }

        return breakdown;
    }
}