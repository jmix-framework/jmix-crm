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
        description = "Calculates and visualizes cashflow risk allocation for business categories by analyzing invoices and payment behavior. Provides a category-level breakdown for risk-focused decision making."
)
@TemplateDef(
        isDefault = true,
        code = "CSV",
        filePath = "com/company/crm/reports/templates/category-cashflow-risk-report.xlsx",
        outputType = ReportOutputType.CSV,
        outputNamePattern = "category-cashflow-risk-report.csv"
)
@TemplateDef(
        code = "XLSX",
        filePath = "com/company/crm/reports/templates/category-cashflow-risk-report.xlsx",
        outputType = ReportOutputType.XLSX,
        outputNamePattern = "category-cashflow-risk-report.xlsx"
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
        name = "Header",
        parent = "Root",
        dataSets = @DataSetDef(name = "header", type = DataSetType.DELEGATE)
)
@BandDef(
        name = "RiskByCategory",
        parent = "Root",
        dataSets = @DataSetDef(name = "riskByCategory", type = DataSetType.DELEGATE)
)
public class CategoryCashflowRiskReport {

    public static final String CODE = "category-cashflow-risk-report";

    @Autowired
    private CategoryCashflowDataLoader categoryCashflowDataLoader;

    @DataSetDelegate(name = "header")
    public ReportDataLoader headerDataLoader() {
        return categoryCashflowDataLoader;
    }

    @DataSetDelegate(name = "riskByCategory")
    public ReportDataLoader riskByCategoryDataLoader() {
        return categoryCashflowDataLoader;
    }
}
