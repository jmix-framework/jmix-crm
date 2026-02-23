package com.company.crm.ai.jmix.report.run;

import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Converts report output documents into LLM-friendly text.
 */
@Component("ai_ReportContentConverter")
public class ReportContentConverter {

    private static final Logger log = LoggerFactory.getLogger(ReportContentConverter.class);

    private static final Set<String> TEXT_OUTPUT_TYPES = Set.of(
            "HTML",
            "CSV",
            "JSON",
            "TEXT"
    );

    /**
     * Extracts text content from a report output document if the format is text-based.
     *
     * @param document   The generated report document
     * @param outputType The output type of the report
     * @return Type-safe conversion result
     */
    public ReportContentResult convert(ReportOutputDocument document, String outputType) {
        if (document == null) {
            return new ReportContentResult.TextContent(null);
        }

        if (isTextOutput(outputType)) {
            byte[] content = document.getContent();
            if (content == null) {
                return new ReportContentResult.TextContent("");
            }
            return new ReportContentResult.TextContent(new String(content, StandardCharsets.UTF_8));
        }

        log.debug("Binary output format detected: {}. Full content not returned to LLM.", outputType);
        return new ReportContentResult.BinaryUnsupported(outputType);
    }

    private boolean isTextOutput(String outputType) {
        if (outputType == null) {
            return false;
        }
        return TEXT_OUTPUT_TYPES.contains(outputType.toUpperCase());
    }
}
