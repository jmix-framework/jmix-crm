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

        String result = converter.convert(document, "HTML");
        assertThat(result).isEqualTo(html);
    }

    @Test
    void testConvertCsv() {
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String csv = "id,name\n1,Test";
        when(document.getContent()).thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        String result = converter.convert(document, "CSV");
        assertThat(result).isEqualTo(csv);
    }

    @Test
    void testConvertPdf() {
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String result = converter.convert(document, "PDF");
        assertThat(result).isEqualTo("BINARY_OUTPUT_NOT_SUPPORTED_YET");
    }

    @Test
    void testConvertNullDocument() {
        String result = converter.convert(null, "HTML");
        assertThat(result).isNull();
    }
}
