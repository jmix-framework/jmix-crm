package com.company.crm.report.dataloader;

import com.company.crm.app.service.client.Client360ReportService;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.report.util.ReportDataLoaderUtils;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import io.jmix.reports.yarg.structure.BandData;
import io.jmix.reports.yarg.structure.ReportQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataLoader for Flags and Indicators section of Client360Report.
 * Provides comprehensive customer classification flags and business indicators.
 */
@Component("flagsAndIndicatorsReportDataLoader")
public class FlagsAndIndicatorsReportDataLoader implements ReportDataLoader {

    private final Client360ReportService client360ReportService;
    private final ClientService clientService;

    public FlagsAndIndicatorsReportDataLoader(Client360ReportService client360ReportService, ClientService clientService) {
        this.client360ReportService = client360ReportService;
        this.clientService = clientService;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        Client client = (Client) params.get("client");
        if (client == null) {
            return List.of(Map.of());
        }

        LocalDateRange dateRange = ReportDataLoaderUtils.getDateRangeFromParams(params);

        // Calculate all flags
        Map<String, Object> flags = new HashMap<>();

        // Customer Classification Flags
        flags.put("isHighValue", client360ReportService.isHighValueCustomer(client, dateRange));
        flags.put("isVIP", client360ReportService.isVIPCustomer(client, dateRange));
        flags.put("isNew", client360ReportService.isNewCustomer(client));
        flags.put("isFrequent", client360ReportService.isFrequentCustomer(client, dateRange));
        flags.put("isInactive", client360ReportService.isInactiveCustomer(client, dateRange));

        // Financial Health Indicators
        flags.put("hasPaymentIssues", client360ReportService.hasPaymentIssues(client, dateRange));
        flags.put("hasGoodPaymentHistory", client360ReportService.hasGoodPaymentHistory(client, dateRange));

        // Outstanding balance check using service
        flags.put("hasOutstandingBalance", client360ReportService.hasOutstandingBalanceFlag(client));
        BigDecimal outstanding = clientService.getOutstandingBalance(client);
        flags.put("outstandingAmount", PriceDataType.defaultFormat(outstanding));

        // Business Relationship Indicators
        flags.put("isBusiness", client360ReportService.isBusinessType(client));
        flags.put("hasAccountManager", client.getAccountManager() != null);

        // Long-term and activity indicators using service
        flags.put("isLongTerm", client360ReportService.isLongTermCustomer(client));
        flags.put("customerTenure", client360ReportService.getCustomerTenure(client));
        flags.put("hasRecentActivity", client360ReportService.hasRecentActivity(client));

        // Sales opportunity from service
        flags.put("hasSalesOpportunity", client360ReportService.hasSalesOpportunity(client, dateRange));

        // Risk assessment from service
        String riskLevel = client360ReportService.calculateRiskLevel(client, dateRange).getId();
        flags.put("isCreditRisk", "HIGH".equals(riskLevel));

        return List.of(flags);
    }
}