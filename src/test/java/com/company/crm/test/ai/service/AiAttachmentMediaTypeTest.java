package com.company.crm.test.ai.service;

import com.company.crm.ai.service.AiAttachmentMediaKind;
import com.company.crm.ai.service.AiAttachmentMediaType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiAttachmentMediaTypeTest {

    @Test
    void resolvesMimeTypeAndKindFromFileName() {
        assertThat(AiAttachmentMediaType.mimeTypeFromFileName("report.CSV")).isEqualTo(Media.Format.DOC_CSV);
        assertThat(AiAttachmentMediaType.mediaKindFromFileName("report.CSV")).isEqualTo(AiAttachmentMediaKind.SPREADSHEET);
        assertThat(AiAttachmentMediaType.mediaKindFromFileName("diagram.png")).isEqualTo(AiAttachmentMediaKind.IMAGE);
        assertThat(AiAttachmentMediaType.mediaKindFromFileName("unknown.bin")).isEqualTo(AiAttachmentMediaKind.OTHER);
    }

    @Test
    void resolvesMimeTypeWithParameters() {
        assertThat(AiAttachmentMediaType.fromMimeType(MimeTypeUtils.parseMimeType("text/plain;charset=UTF-8")))
                .contains(AiAttachmentMediaType.TXT);
    }

    @Test
    void exposesBusinessQuestionsForModelInput() {
        assertThat(AiAttachmentMediaType.PNG.isImage()).isTrue();
        assertThat(AiAttachmentMediaType.CSV.isTextContextReadable()).isTrue();
        assertThat(AiAttachmentMediaType.PDF.isTextContextReadable()).isFalse();
        assertThat(AiAttachmentMediaType.PDF.isSupported()).isTrue();
    }
}
