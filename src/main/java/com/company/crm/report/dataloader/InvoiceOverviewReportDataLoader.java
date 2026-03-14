package com.company.crm.report.dataloader;

import com.company.crm.app.service.client.Client360ReportService;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PercentDataType;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.report.util.ReportDataLoaderUtils;
import io.jmix.core.DataManager;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
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
 * DataLoader for Invoice Overview section of Client360Report.
 * Provides financial overview including total invoiced, paid, outstanding balance and payment rate.
 */
@Component("invoiceOverviewReportDataLoader")
public class InvoiceOverviewReportDataLoader implements ReportDataLoader {

    private final ClientService clientService;
    private final Client360ReportService client360ReportService;
    private final DataManager dataManager;
    private final DatatypeFormatter datatypeFormatter;

    public InvoiceOverviewReportDataLoader(ClientService clientService,
                                           Client360ReportService client360ReportService,
                                           DataManager dataManager, DatatypeFormatter datatypeFormatter) {
        this.clientService = clientService;
        this.client360ReportService = client360ReportService;
        this.dataManager = dataManager;
        this.datatypeFormatter = datatypeFormatter;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        LocalDateRange dateRange = ReportDataLoaderUtils.getDateRangeFromParams(params);
        UUID clientId = client.getId();

        // Use ClientService for consistent calculations
        BigDecimal totalInvoiced = clientService.getInvoicesTotalSum(client);
        BigDecimal totalPaid = clientService.getPaymentsTotalSum(client);
        BigDecimal outstanding = clientService.getOutstandingBalance(client);

        Long invoiceCount = dataManager.loadValue("SELECT COUNT(i) FROM Invoice i " +
                "WHERE i.client.id = :clientId AND i.date BETWEEN :fromDate AND :toDate", Long.class)
                .parameter("clientId", clientId)
                .parameter("fromDate", dateRange.startDate())
                .parameter("toDate", dateRange.endDate())
                .one();

        // Payment rate calculation
        double paymentRate = client360ReportService.calculatePaymentRate(totalInvoiced, totalPaid);

        Map<String, Object> fields = new HashMap<>();
        fields.put("totalInvoiceCount", invoiceCount);
        fields.put("totalInvoiced", PriceDataType.defaultFormat(totalInvoiced, datatypeFormatter));
        fields.put("totalPaid", PriceDataType.defaultFormat(totalPaid, datatypeFormatter));
        fields.put("outstanding", PriceDataType.defaultFormat(outstanding, datatypeFormatter));
        fields.put("paymentRate", new PercentDataType().format(BigDecimal.valueOf(paymentRate)));

        return List.of(fields);
    }
}