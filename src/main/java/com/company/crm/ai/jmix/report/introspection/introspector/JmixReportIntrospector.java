package com.company.crm.ai.jmix.report.introspection.introspector;

import com.company.crm.ai.jmix.report.introspection.model.*;
import io.jmix.reports.ReportRepository;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportInputParameter;
import io.jmix.reports.entity.ReportTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Introspects Jmix Reports and converts them into AI-optimized descriptors.
 * Uses a list of {@link ReportParameterIntrospector} strategies to map report parameters.
 */
@Component("ai_JmixReportIntrospector")
public class JmixReportIntrospector {

    private final ReportRepository reportRepository;
    private final List<ReportParameterIntrospector> parameterIntrospectors;

    public JmixReportIntrospector(ReportRepository reportRepository, 
                                  List<ReportParameterIntrospector> parameterIntrospectors) {
        this.reportRepository = reportRepository;
        this.parameterIntrospectors = parameterIntrospectors;
    }

    /**
     * Introspects all available reports.
     *
     * @return AiReportModelDescriptor containing all discovered reports
     */
    public AiReportModelDescriptor introspect() {
        return introspectInternal(reportRepository.getAllReports());
    }

    /**
     * Introspects specified reports by their codes.
     *
     * @param reportCodes collection of report codes to include
     * @return AiReportModelDescriptor containing requested reports
     */
    public AiReportModelDescriptor introspect(Collection<String> reportCodes) {
        if (reportCodes == null || reportCodes.isEmpty()) {
            return new AiReportModelDescriptor(Collections.emptyMap());
        }

        List<Report> filteredReports = reportRepository.getAllReports().stream()
                .filter(r -> reportCodes.contains(r.getCode()))
                .collect(Collectors.toList());

        return introspectInternal(filteredReports);
    }

    private AiReportModelDescriptor introspectInternal(Collection<Report> reports) {
        Map<String, AiReportDescriptor> reportDescriptors = reports.stream()
                .map(reportRepository::reloadForRunning)
                .collect(Collectors.toMap(
                        Report::getCode,
                        this::mapToDescriptor,
                        (existing, replacement) -> existing
                ));

        return new AiReportModelDescriptor(reportDescriptors);
    }

    private AiReportDescriptor mapToDescriptor(Report report) {
        String groupName = report.getGroup() != null ? report.getGroup().getTitle() : null;

        List<AiReportTemplateDescriptor> templates = report.getTemplates() != null ?
                report.getTemplates().stream()
                        .map(t -> mapTemplate(report, t))
                        .collect(Collectors.toList()) : Collections.emptyList();

        List<AiReportParameterDescriptor> parameters = report.getInputParameters() != null ?
                report.getInputParameters().stream()
                        .map(this::mapParameter)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()) : Collections.emptyList();

        return new AiReportDescriptor(
                report.getCode(),
                report.getName(),
                report.getDescription(),
                groupName,
                templates,
                parameters
        );
    }

    private AiReportTemplateDescriptor mapTemplate(Report report, ReportTemplate template) {
        return new AiReportTemplateDescriptor(
                template.getCode(),
                template.getReportOutputType() != null ? template.getReportOutputType().toString() : null,
                Objects.equals(report.getDefaultTemplate(), template)
        );
    }

    private AiReportParameterDescriptor mapParameter(ReportInputParameter parameter) {
        for (ReportParameterIntrospector introspector : parameterIntrospectors) {
            if (introspector.supports(parameter)) {
                return introspector.introspect(parameter);
            }
        }
        return null;
    }
}
