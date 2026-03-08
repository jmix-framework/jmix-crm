package com.company.crm.ai.report.run;

import com.company.crm.ai.report.run.ReportContentConverter;
import com.company.crm.ai.report.run.ReportContentResult;
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
        // given
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String html = "<html><body>Test</body></html>";
        when(document.getContent()).thenReturn(html.getBytes(StandardCharsets.UTF_8));

        // when
        ReportContentResult result = converter.convert(document, "HTML");

        // then
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        assertThat(((ReportContentResult.TextContent) result).content()).isEqualTo(html);
    }

    @Test
    void testConvertCsv() {
        // given
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String csv = "id,name\n1,Test";
        when(document.getContent()).thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        // when
        ReportContentResult result = converter.convert(document, "CSV");

        // then
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        assertThat(((ReportContentResult.TextContent) result).content()).isEqualTo(csv);
    }

    @Test
    void testConvertHtml_caseInsensitiveOutputType() {
        // given
        ReportOutputDocument document = mock(ReportOutputDocument.class);
        String html = "<html><body>Case Test</body></html>";
        when(document.getContent()).thenReturn(html.getBytes(StandardCharsets.UTF_8));

        // when
        ReportContentResult result = converter.convert(document, "html");

        // then
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        assertThat(((ReportContentResult.TextContent) result).content()).isEqualTo(html);
    }

    @Test
    void testConvertPdf() {
        // given
        ReportOutputDocument document = mock(ReportOutputDocument.class);

        // when
        ReportContentResult result = converter.convert(document, "PDF");

        // then
        assertThat(result).isInstanceOf(ReportContentResult.BinaryUnsupported.class);
        ReportContentResult.BinaryUnsupported unsupported = (ReportContentResult.BinaryUnsupported) result;
        assertThat(unsupported.outputType()).isEqualTo("PDF");
        assertThat(unsupported.outputType()).isNotBlank();
    }

    @Test
    void testConvertNullOutputType_returnsBinaryUnsupported() {
        // given
        ReportOutputDocument document = mock(ReportOutputDocument.class);

        // when
        ReportContentResult result = converter.convert(document, null);

        // then
        assertThat(result).isInstanceOf(ReportContentResult.BinaryUnsupported.class);
        ReportContentResult.BinaryUnsupported unsupported = (ReportContentResult.BinaryUnsupported) result;
        assertThat(unsupported.outputType()).isNull();
    }

    @Test
    void testConvertNullDocument_returnsNullTextContentByContract() {
        // given
        // Null document is intentionally mapped to TextContent(null), so callers can keep one text-based control flow.

        // when
        ReportContentResult result = converter.convert(null, "HTML");

        // then
        assertThat(result).isInstanceOf(ReportContentResult.TextContent.class);
        ReportContentResult.TextContent textContent = (ReportContentResult.TextContent) result;
        assertThat(textContent.content()).isNull();
    }
}
