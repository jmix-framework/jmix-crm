package com.company.crm.report;

import com.company.crm.model.client.Client;
import com.company.crm.report.dataloader.CategoryCashflowDataLoader;
import io.jmix.reports.annotation.*;
import io.jmix.reports.entity.DataSetType;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.yarg.loaders.ReportDataLoader;
import org.springframework.beans.factory.annotation.Autowired;

@ReportDef(
        code = CategoryCashflowRiskReport.CODE,
        name = "Category Cashflow Risk Allocation Report",
        description = "Calculates and visualizes cashflow risk for business categories by analyzing outstanding invoices. It identifies critical invoices and provides a risk allocation breakdown by category, client, and time period."
)
@TemplateDef(
        isDefault = true,
        code = "HTML",
        filePath = "com/company/crm/report/category-cashflow-risk-report.html",
        outputType = ReportOutputType.HTML,
        outputNamePattern = "category-cashflow-risk-report.html",
        templateEngine = TemplateMarkupEngine.FREEMARKER
)
@InputParameterDef(
        alias = "fromDate",
        name = "From Date",
        type = ParameterType.DATE
)
@InputParameterDef(
        alias = "toDate",
        name = "To Date",
        type = ParameterType.DATE
)
@InputParameterDef(
        alias = "client",
        name = "Client",
        type = ParameterType.ENTITY,
        entity = @EntityParameterDef(entityClass = Client.class)
)
@InputParameterDef(
        alias = "asOfDate",
        name = "As of Date",
        type = ParameterType.DATE
)
@InputParameterDef(
        alias = "includePaid",
        name = "Include Paid Invoices",
        type = ParameterType.BOOLEAN
)
@BandDef(
        name = "Root",
        root = true
)
@BandDef(
        name = "RiskByCategory",
        parent = "Root",
        dataSets = @DataSetDef(name = "riskByCategory", type = DataSetType.DELEGATE)
)
@BandDef(
        name = "CriticalInvoices",
        parent = "Root",
        dataSets = @DataSetDef(name = "criticalInvoices", type = DataSetType.DELEGATE)
)
public class CategoryCashflowRiskReport {

    public static final String CODE = "category-cashflow-risk-report";

    @Autowired
    private CategoryCashflowDataLoader categoryCashflowDataLoader;

    @DataSetDelegate(name = "riskByCategory")
    public ReportDataLoader riskByCategoryDataLoader() {
        return categoryCashflowDataLoader;
    }

    @DataSetDelegate(name = "criticalInvoices")
    public ReportDataLoader criticalInvoicesDataLoader() {
        return categoryCashflowDataLoader;
    }
}
