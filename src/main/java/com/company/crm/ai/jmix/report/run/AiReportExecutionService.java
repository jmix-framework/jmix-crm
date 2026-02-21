package com.company.crm.ai.jmix.report.run;

import io.jmix.reports.ReportRepository;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.runner.ReportRunner;
import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Service for executing Jmix reports on behalf of AI tools.
 */
@Service("ai_AiReportExecutionService")
public class AiReportExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AiReportExecutionService.class);

    private final ReportRepository reportRepository;
    private final ReportRunner reportRunner;
    private final AiReportParameterConverter parameterConverter;
    private final ReportContentConverter contentConverter;

    public AiReportExecutionService(ReportRepository reportRepository,
                                   ReportRunner reportRunner,
                                   AiReportParameterConverter parameterConverter,
                                   ReportContentConverter contentConverter) {
        this.reportRepository = reportRepository;
        this.reportRunner = reportRunner;
        this.parameterConverter = parameterConverter;
        this.contentConverter = contentConverter;
    }

    /**
     * Executes a report by its code with provided parameters.
     *
     * @param reportCode          Unique code of the report to run
     * @param parameters          Input parameters provided by LLM
     * @param templateCode        Optional template code. If null, default template is used.
     * @param outputType          Optional output type override.
     * @param allowedReportCodes Mandatory whitelist of allowed report codes.
     * @return Execution result with content or error details
     */
    public ReportExecutionResult executeReport(String reportCode, Map<String, Object> parameters, String templateCode, String outputType, Collection<String> allowedReportCodes) {
        try {
            // 0. Mandatory Whitelist Guard
            if (allowedReportCodes == null || !allowedReportCodes.contains(reportCode)) {
                return ReportExecutionResult.failed(reportCode, "ACCESS_DENIED", "Report execution is not allowed for this report code. Ensure it is whitelisted.");
            }

            // 1. Load report
            Report report = reportRepository.getAllReports().stream()
                    .filter(r -> reportCode.equals(r.getCode()))
                    .findFirst()
                    .orElse(null);

            if (report == null) {
                return ReportExecutionResult.failed(reportCode, "REPORT_NOT_FOUND", "Report with code '" + reportCode + "' not found.");
            }

            // Reload to get all details (parameters, templates)
            report = reportRepository.reloadForRunning(report);

            // 3. Resolve Template
            ReportTemplate template = resolveTemplate(report, templateCode);
            if (templateCode != null && template == null) {
                return ReportExecutionResult.failed(reportCode, "TEMPLATE_NOT_FOUND", "Template with code '" + templateCode + "' not found for this report.");
            }

            String effectiveTemplateCode = template != null ? template.getCode() : null;
            String effectiveOutputType = outputType != null ? outputType : (template != null && template.getReportOutputType() != null ? template.getReportOutputType().toString() : null);

            // 4. Convert and Validate Parameters
            ReportParameterConversionResult conversionResult = parameterConverter.convertParameters(report.getInputParameters(), parameters);
            if (!conversionResult.success()) {
                if (conversionResult.hasConversionErrors()) {
                    return ReportExecutionResult.parameterConversionError(reportCode, conversionResult.errors());
                }
                return ReportExecutionResult.validationError(reportCode, conversionResult.errors());
            }

            // 5. Run Report
            var runner = reportRunner.byReportEntity(report)
                    .withParams(conversionResult.convertedParameters());

            if (effectiveTemplateCode != null) {
                runner.withTemplateCode(effectiveTemplateCode);
            }
            if (outputType != null) {
                // Note: ReportRunner.withOutputType expects io.jmix.reports.entity.ReportOutputType enum
                try {
                    runner.withOutputType(io.jmix.reports.entity.ReportOutputType.valueOf(outputType.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ReportExecutionResult.failed(reportCode, "INVALID_OUTPUT_TYPE", "Output type '" + outputType + "' is not supported.");
                }
            }

            ReportOutputDocument document = runner.run();

            // 6. Convert Output to Text
            String content = contentConverter.convert(document, effectiveOutputType);

            if ("BINARY_OUTPUT_NOT_SUPPORTED_YET".equals(content)) {
                return ReportExecutionResult.failed(reportCode, "BINARY_OUTPUT_NOT_SUPPORTED_YET", "Binary output formats (like PDF, XLSX) are not yet supported for LLM analysis.");
            }

            return ReportExecutionResult.success(reportCode, effectiveTemplateCode, effectiveOutputType, content);

        } catch (Exception e) {
            log.error("Failed to execute report {}", reportCode, e);
            return ReportExecutionResult.failed(reportCode, "EXECUTION_ERROR", "An unexpected error occurred during report execution: " + e.getMessage());
        }
    }

    private ReportTemplate resolveTemplate(Report report, String templateCode) {
        if (templateCode == null) {
            return report.getDefaultTemplate();
        }
        return report.getTemplates().stream()
                .filter(t -> templateCode.equals(t.getCode()))
                .findFirst()
                .orElse(null);
    }
}
