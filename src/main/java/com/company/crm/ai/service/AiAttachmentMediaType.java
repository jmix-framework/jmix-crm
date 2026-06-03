package com.company.crm.ai.service;

import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum AiAttachmentMediaType {
    PDF(Media.Format.DOC_PDF, AiAttachmentMediaKind.TEXT_DOCUMENT, false, false, ".pdf"),
    CSV(Media.Format.DOC_CSV, AiAttachmentMediaKind.SPREADSHEET, false, true, ".csv"),
    DOC(Media.Format.DOC_DOC, AiAttachmentMediaKind.TEXT_DOCUMENT, false, false, ".doc"),
    DOCX(Media.Format.DOC_DOCX, AiAttachmentMediaKind.TEXT_DOCUMENT, false, false, ".docx"),
    XLS(Media.Format.DOC_XLS, AiAttachmentMediaKind.SPREADSHEET, false, false, ".xls"),
    XLSX(Media.Format.DOC_XLSX, AiAttachmentMediaKind.SPREADSHEET, false, false, ".xlsx"),
    HTML(Media.Format.DOC_HTML, AiAttachmentMediaKind.TEXT_DOCUMENT, false, true, ".html", ".htm"),
    TXT(Media.Format.DOC_TXT, AiAttachmentMediaKind.TEXT_DOCUMENT, false, true, ".txt"),
    MARKDOWN(Media.Format.DOC_MD, AiAttachmentMediaKind.TEXT_DOCUMENT, false, true, ".md"),
    JSON(MimeTypeUtils.APPLICATION_JSON, AiAttachmentMediaKind.JSON, false, true, ".json"),
    PNG(Media.Format.IMAGE_PNG, AiAttachmentMediaKind.IMAGE, true, false, ".png"),
    JPEG(Media.Format.IMAGE_JPEG, AiAttachmentMediaKind.IMAGE, true, false, ".jpg", ".jpeg"),
    GIF(Media.Format.IMAGE_GIF, AiAttachmentMediaKind.IMAGE, true, false, ".gif"),
    WEBP(Media.Format.IMAGE_WEBP, AiAttachmentMediaKind.IMAGE, true, false, ".webp");

    private final MimeType mimeType;
    private final AiAttachmentMediaKind mediaKind;
    private final boolean image;
    private final boolean textContextReadable;
    private final List<String> extensions;

    AiAttachmentMediaType(MimeType mimeType,
                          AiAttachmentMediaKind mediaKind,
                          boolean image,
                          boolean textContextReadable,
                          String... extensions) {
        this.mimeType = mimeType;
        this.mediaKind = mediaKind;
        this.image = image;
        this.textContextReadable = textContextReadable;
        this.extensions = List.of(extensions);
    }

    public MimeType mimeType() {
        return mimeType;
    }

    public AiAttachmentMediaKind mediaKind() {
        return mediaKind;
    }

    public boolean isImage() {
        return image;
    }

    public boolean isTextContextReadable() {
        return textContextReadable;
    }

    public boolean isSupported() {
        return true;
    }

    public static Optional<AiAttachmentMediaType> fromMimeType(MimeType mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(candidate -> candidate.matches(mimeType))
                .findFirst();
    }

    public static Optional<AiAttachmentMediaType> fromFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return Optional.empty();
        }
        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(candidate -> candidate.extensions.stream().anyMatch(normalizedFileName::endsWith))
                .findFirst();
    }

    public static MimeType mimeTypeFromFileName(String fileName) {
        return fromFileName(fileName)
                .map(AiAttachmentMediaType::mimeType)
                .orElse(null);
    }

    public static AiAttachmentMediaKind mediaKindFromFileName(String fileName) {
        return fromFileName(fileName)
                .map(AiAttachmentMediaType::mediaKind)
                .orElse(AiAttachmentMediaKind.OTHER);
    }

    private boolean matches(MimeType other) {
        return mimeType.getType().equalsIgnoreCase(other.getType())
                && mimeType.getSubtype().equalsIgnoreCase(other.getSubtype());
    }
}
