package com.company.crm.app.util.report;

import com.company.crm.model.invoice.Invoice;
import com.company.crm.report.InvoiceReport;
import io.jmix.reports.runner.ReportRunner;
import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import io.jmix.reportsflowui.download.ReportDownloader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CrmReportUtils {

    private final ReportRunner reportRunner;
    private final ObjectProvider<ReportDownloader> reportDownloaderProvider;

    public CrmReportUtils(ReportRunner reportRunner, ObjectProvider<ReportDownloader> reportDownloaderProvider) {
        this.reportRunner = reportRunner;
        this.reportDownloaderProvider = reportDownloaderProvider;
    }

    public void runAndDownloadReport(String code, Map<String, Object> params) {
        ReportOutputDocument report = reportRunner.byReportCode(code).withParams(params).run();
        reportDownloaderProvider.getObject().download(report.getContent(), report.getDocumentName());
    }

    public void runAndDownloadReport(Invoice invoice) {
        runAndDownloadReport(InvoiceReport.CODE, Map.of(InvoiceReport.PARAM_INVOICE, invoice));
    }
}
