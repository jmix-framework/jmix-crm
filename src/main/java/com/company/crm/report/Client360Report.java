package com.company.crm.report;

import com.company.crm.model.client.Client;
import java.util.Objects;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.reports.annotation.*;
import io.jmix.reports.entity.DataSetType;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReportDef(
        code = "client-360-report",
        name = "Client 360 Report",
        description = "Comprehensive client report with orders, invoices, payments and activities"
)

@TemplateDef(
        isDefault = true,
        code = "HTML",
        filePath = "com/company/crm/report/client-360-report.html",
        outputType = ReportOutputType.HTML,
        outputNamePattern = "client-360-report.html",
        templateEngine = TemplateMarkupEngine.FREEMARKER
)

@InputParameterDef(
        alias = "client",
        name = "Client",
        type = ParameterType.ENTITY,
        required = true,
        hidden = true,
        entity = @EntityParameterDef(entityClass = Client.class)
)

@InputParameterDef(
        alias = "fromDate",
        name = "From Date",
        type = ParameterType.DATE,
        required = true,
        hidden = true
)

@InputParameterDef(
        alias = "toDate",
        name = "To Date",
        type = ParameterType.DATE,
        required = true,
        hidden = true
)

@InputParameterDef(
        alias = "audience",
        name = "Audience",
        type = ParameterType.TEXT,
        hidden = true,
        defaultValue = "default"
)

@BandDef(
        name = "Root",
        root = true,
        dataSets = @DataSetDef(name = "root", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "Client",
        parent = "Root",
        dataSets = @DataSetDef(name = "client", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "Orders",
        parent = "Root",
        dataSets = @DataSetDef(name = "orders", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "InvoiceOverview",
        parent = "Root",
        dataSets = @DataSetDef(name = "invoiceOverview", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "InvoiceStatusBreakdown",
        parent = "Root",
        dataSets = @DataSetDef(name = "invoiceStatusBreakdown", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "PaymentHistory",
        parent = "Root",
        dataSets = @DataSetDef(name = "paymentHistory", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "RiskIndicators",
        parent = "Root",
        dataSets = @DataSetDef(name = "riskIndicators", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "FlagsAndIndicators",
        parent = "Root",
        dataSets = @DataSetDef(name = "flagsAndIndicators", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "Contacts",
        parent = "Root",
        dataSets = @DataSetDef(name = "contacts", type = DataSetType.DELEGATE)
)

@BandDef(
        name = "RecentActivities",
        parent = "Root",
        dataSets = @DataSetDef(name = "recentActivities", type = DataSetType.DELEGATE)
)


/**
 * Streamlined Client 360 Report that delegates complex data loading to dedicated DataLoader beans.
 * This approach improves maintainability, testability, and separation of concerns.
 */
public class Client360Report {

    // Minimal dependencies - only for simple coordination
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Autowired
    private MetadataTools metadataTools;

    // External DataLoader beans
    @Autowired
    @Qualifier("ordersReportDataLoader")
    private ReportDataLoader ordersDataLoader;

    @Autowired
    @Qualifier("paymentHistoryReportDataLoader")
    private ReportDataLoader paymentHistoryDataLoader;

    @Autowired
    @Qualifier("contactsReportDataLoader")
    private ReportDataLoader contactsDataLoader;

    @Autowired
    @Qualifier("recentActivitiesReportDataLoader")
    private ReportDataLoader recentActivitiesDataLoader;

    @Autowired
    @Qualifier("invoiceStatusBreakdownReportDataLoader")
    private ReportDataLoader invoiceStatusBreakdownDataLoader;

    @Autowired
    @Qualifier("invoiceOverviewReportDataLoader")
    private ReportDataLoader invoiceOverviewDataLoader;

    @Autowired
    @Qualifier("riskIndicatorsReportDataLoader")
    private ReportDataLoader riskIndicatorsDataLoader;

    @Autowired
    @Qualifier("flagsAndIndicatorsReportDataLoader")
    private ReportDataLoader flagsAndIndicatorsDataLoader;

    @DataSetDelegate(name = "root")
    public ReportDataLoader rootDataLoader() {
        return (reportQuery, parentBand, params) -> {
            LocalDate fromDate = convertToLocalDate((Date) params.get("fromDate"));
            LocalDate toDate = convertToLocalDate((Date) params.get("toDate"));

            Map<String, Object> rootData = new HashMap<>();
            rootData.put("generatedAt", datatypeFormatter.formatLocalDateTime(LocalDateTime.now()));
            rootData.put("fromDate", datatypeFormatter.formatLocalDate(fromDate));
            rootData.put("toDate", datatypeFormatter.formatLocalDate(toDate));
            rootData.put("dateRange", datatypeFormatter.formatLocalDate(fromDate) + " - " + datatypeFormatter.formatLocalDate(toDate));
            rootData.put("audience", params.get("audience") != null ? params.get("audience") : "default");

            return List.of(rootData);
        };
    }

    @DataSetDelegate(name = "client")
    public ReportDataLoader clientDataLoader() {
        return (reportQuery, parentBand, params) -> {
            Client client = (Client) params.get("client");
            if (client == null) {
                return List.of(Map.of("name", "No client selected"));
            }

            Map<String, Object> fields = new HashMap<>();
            fields.put("name", Objects.toString(client.getName(), ""));
            fields.put("fullName", Objects.toString(client.getFullName(), ""));
            fields.put("type", metadataTools.format(client.getType()));
            fields.put("regNumber", Objects.toString(client.getRegNumber(), ""));
            fields.put("vatNumber", Objects.toString(client.getVatNumber(), ""));
            fields.put("website", Objects.toString(client.getWebsite(), ""));
            fields.put("address", metadataTools.getInstanceName(client.getAddress()));
            fields.put("accountManager", client.getAccountManager() != null ?
                       metadataTools.getInstanceName(client.getAccountManager()) : "");
            return List.of(fields);
        };
    }

    // Delegate to external DataLoader beans
    @DataSetDelegate(name = "orders")
    public ReportDataLoader ordersDataLoader() {
        return ordersDataLoader;
    }



    private LocalDate convertToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }


    @DataSetDelegate(name = "invoiceOverview")
    public ReportDataLoader invoiceOverviewDataLoader() {
        return invoiceOverviewDataLoader;
    }

    @DataSetDelegate(name = "invoiceStatusBreakdown")
    public ReportDataLoader invoiceStatusBreakdownDataLoader() {
        return invoiceStatusBreakdownDataLoader;
    }

    @DataSetDelegate(name = "paymentHistory")
    public ReportDataLoader paymentHistoryDataLoader() {
        return paymentHistoryDataLoader;
    }

    @DataSetDelegate(name = "riskIndicators")
    public ReportDataLoader riskIndicatorsDataLoader() {
        return riskIndicatorsDataLoader;
    }

    @DataSetDelegate(name = "flagsAndIndicators")
    public ReportDataLoader flagsAndIndicatorsDataLoader() {
        return flagsAndIndicatorsDataLoader;
    }

    @DataSetDelegate(name = "contacts")
    public ReportDataLoader contactsDataLoader() {
        return contactsDataLoader;
    }

    @DataSetDelegate(name = "recentActivities")
    public ReportDataLoader recentActivitiesDataLoader() {
        return recentActivitiesDataLoader;
    }



}