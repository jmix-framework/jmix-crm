package com.company.crm.report.dataloader;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.payment.Payment;
import com.company.crm.report.mapper.ReportPaymentMapper;
import com.company.crm.report.util.ReportDataLoaderUtils;
import io.jmix.core.DataManager;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DataLoader for Payment History section of Client360Report.
 * Loads the most recent payments for a client within the specified date range.
 * Limited to 10 most recent payments.
 */
@Component("paymentHistoryReportDataLoader")
public class PaymentHistoryReportDataLoader implements ReportDataLoader {

    private final DataManager dataManager;
    private final ReportPaymentMapper paymentMapper;

    public PaymentHistoryReportDataLoader(DataManager dataManager, ReportPaymentMapper paymentMapper) {
        this.dataManager = dataManager;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        LocalDateRange dateRange = ReportDataLoaderUtils.getDateRangeFromParams(params);
        UUID clientId = client.getId();

        List<Payment> payments = dataManager.load(Payment.class)
                .query("SELECT p FROM Payment p " +
                       "WHERE p.invoice.client.id = :clientId " +
                       "AND p.date BETWEEN :fromDate AND :toDate " +
                       "ORDER BY p.date DESC")
                .parameter("clientId", clientId)
                .parameter("fromDate", dateRange.startDate())
                .parameter("toDate", dateRange.endDate())
                .fetchPlanProperties("number", "date", "amount", "invoice.number")
                .maxResults(10)
                .list();

        return payments.stream()
                .map(paymentMapper::toReportMap)
                .toList();
    }
}