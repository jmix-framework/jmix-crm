package com.company.crm.ai.jmix.report.run;

import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportContentConverterTest {

    private final ReportContentConverter converter = new ReportContentConverter();

    @Test
    void testConvertHtml() {
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String html = "<html><body>Test</body></html>";
        when(document.getContent()).thenReturn(html.getBytes(StandardCharsets.UTF_8));

        ReportContentResult result = converter.convert(document, "HTML");
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        assertThat(((ReportContentResult.TextContent) result).content()).isEqualTo(html);
    }

    @Test
    void testConvertCsv() {
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String csv = "id,name\n1,Test";
        when(document.getContent()).thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        ReportContentResult result = converter.convert(document, "CSV");
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        assertThat(((ReportContentResult.TextContent) result).content()).isEqualTo(csv);
    }

    @Test
    void testConvertHtml_caseInsensitiveOutputType() {
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String html = "<html><body>Case Test</body></html>";
        when(document.getContent()).thenReturn(html.getBytes(StandardCharsets.UTF_8));

        ReportContentResult result = converter.convert(document, "html");
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        assertThat(((ReportContentResult.TextContent) result).content()).isEqualTo(html);
    }

    @Test
    void testConvertPdf() {
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        ReportContentResult result = converter.convert(document, "PDF");
        assertThat(result).isInstanceOf(ReportContentResult.BinaryUnsupported.class);
        assertThat(((ReportContentResult.BinaryUnsupported) result).outputType()).isEqualTo("PDF");
    }

    @Test
    void testConvertNullOutputType_returnsBinaryUnsupported() {
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        ReportContentResult result = converter.convert(document, null);
        assertThat(result).isInstanceOf(ReportContentResult.BinaryUnsupported.class);
        assertThat(((ReportContentResult.BinaryUnsupported) result).outputType()).isNull();
    }

    @Test
    void testConvertNullDocument() {
        ReportContentResult result = converter.convert(null, "HTML");
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        assertThat(((ReportContentResult.TextContent) result).content()).isNull();
    }
}
