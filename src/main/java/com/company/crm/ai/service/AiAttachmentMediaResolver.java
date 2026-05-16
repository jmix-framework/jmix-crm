package com.company.crm.ai.service;

import com.company.crm.ai.model.AiConversationAttachment;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves AI conversation attachments into Spring AI {@link Media} objects for LLM input.
 */
@Service
public class AiAttachmentMediaResolver {

    private static final int MAX_MEDIA_NAME_LENGTH = 96;
    private static final int MAX_TEXT_CONTEXT_LENGTH = 60_000;

    private static final Set<MimeType> IMAGE_MEDIA_TYPES = Set.of(
            Media.Format.IMAGE_PNG,
            Media.Format.IMAGE_JPEG,
            Media.Format.IMAGE_GIF,
            Media.Format.IMAGE_WEBP
    );

    private static final Set<MimeType> TEXT_CONTEXT_TYPES = Set.of(
            Media.Format.DOC_CSV,
            Media.Format.DOC_HTML,
            Media.Format.DOC_TXT,
            Media.Format.DOC_MD,
            MimeTypeUtils.APPLICATION_JSON
    );

    private static final Set<MimeType> SUPPORTED_ATTACHMENT_TYPES = Set.of(
            Media.Format.DOC_PDF,
            Media.Format.DOC_CSV,
            Media.Format.DOC_DOC,
            Media.Format.DOC_DOCX,
            Media.Format.DOC_XLS,
            Media.Format.DOC_XLSX,
            Media.Format.DOC_HTML,
            Media.Format.DOC_TXT,
            Media.Format.DOC_MD,
            MimeTypeUtils.APPLICATION_JSON,
            Media.Format.IMAGE_PNG,
            Media.Format.IMAGE_JPEG,
            Media.Format.IMAGE_GIF,
            Media.Format.IMAGE_WEBP
    );

    private static final Set<MimeType> SPREADSHEET_TYPES = Set.of(
            Media.Format.DOC_CSV,
            Media.Format.DOC_XLS,
            Media.Format.DOC_XLSX
    );

    private static final Set<MimeType> TEXT_DOCUMENT_TYPES = Set.of(
            Media.Format.DOC_PDF,
            Media.Format.DOC_HTML,
            Media.Format.DOC_TXT,
            Media.Format.DOC_MD
    );

    private static final Map<String, MimeType> EXTENSION_MIME_TYPES = Map.ofEntries(
            Map.entry(".pdf", Media.Format.DOC_PDF),
            Map.entry(".csv", Media.Format.DOC_CSV),
            Map.entry(".doc", Media.Format.DOC_DOC),
            Map.entry(".docx", Media.Format.DOC_DOCX),
            Map.entry(".xls", Media.Format.DOC_XLS),
            Map.entry(".xlsx", Media.Format.DOC_XLSX),
            Map.entry(".html", Media.Format.DOC_HTML),
            Map.entry(".htm", Media.Format.DOC_HTML),
            Map.entry(".txt", Media.Format.DOC_TXT),
            Map.entry(".md", Media.Format.DOC_MD),
            Map.entry(".json", MimeTypeUtils.APPLICATION_JSON),
            Map.entry(".png", Media.Format.IMAGE_PNG),
            Map.entry(".jpg", Media.Format.IMAGE_JPEG),
            Map.entry(".jpeg", Media.Format.IMAGE_JPEG),
            Map.entry(".gif", Media.Format.IMAGE_GIF),
            Map.entry(".webp", Media.Format.IMAGE_WEBP)
    );

    private final FileStorageLocator fileStorageLocator;

    public AiAttachmentMediaResolver(FileStorageLocator fileStorageLocator) {
        this.fileStorageLocator = fileStorageLocator;
    }

    public ResolvedAttachmentInput resolve(AiConversationAttachment attachment, String mimeTypeHint) {
        String fileName = attachment.getFileName();
        FileRef fileRef = attachment.getFile();
        if (fileRef == null) {
            throw new IllegalArgumentException("Attachment has no file payload: " + attachment.getId());
        }
        byte[] data = readFileBytes(fileRef);
        MimeType mimeType = resolveSupportedMimeType(mimeTypeHint, fileName);

        if (IMAGE_MEDIA_TYPES.contains(mimeType)) {
            Media media = Media.builder()
                    .mimeType(mimeType)
                    .data(data)
                    .name(sanitizeMediaName(fileName))
                    .build();
            return new ResolvedAttachmentInput(List.of(media), null);
        }

        return new ResolvedAttachmentInput(List.of(), buildTextContext(fileName, mimeType, data));
    }

    private byte[] readFileBytes(FileRef fileRef) {
        FileStorage fileStorage = fileStorageLocator.getByName(fileRef.getStorageName());
        try (InputStream inputStream = fileStorage.openStream(fileRef)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read attachment from file storage: " + fileRef.getFileName(), e);
        }
    }

    private MimeType resolveSupportedMimeType(String rawMimeType, String fileName) {
        MimeType parsed = tryParseMimeType(rawMimeType);
        if (parsed != null && SUPPORTED_ATTACHMENT_TYPES.contains(parsed)) {
            return parsed;
        }

        MimeType fromExtension = mimeTypeFromFileName(fileName);
        if (fromExtension != null && SUPPORTED_ATTACHMENT_TYPES.contains(fromExtension)) {
            return fromExtension;
        }

        throw new IllegalArgumentException("Unsupported attachment media type for model input: " + fileName);
    }

    private MimeType tryParseMimeType(String rawMimeType) {
        if (!StringUtils.hasText(rawMimeType)) {
            return null;
        }
        try {
            return MimeTypeUtils.parseMimeType(rawMimeType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static MimeType mimeTypeFromFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        return EXTENSION_MIME_TYPES.entrySet().stream()
                .filter(entry -> normalized.endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static AttachmentMediaKind mediaKindFromFileName(String fileName) {
        MimeType mimeType = mimeTypeFromFileName(fileName);
        if (SPREADSHEET_TYPES.contains(mimeType)) {
            return AttachmentMediaKind.SPREADSHEET;
        }
        if (TEXT_DOCUMENT_TYPES.contains(mimeType)) {
            return AttachmentMediaKind.TEXT_DOCUMENT;
        }
        if (MimeTypeUtils.APPLICATION_JSON.equals(mimeType)) {
            return AttachmentMediaKind.JSON;
        }
        if (IMAGE_MEDIA_TYPES.contains(mimeType)) {
            return AttachmentMediaKind.IMAGE;
        }
        return AttachmentMediaKind.OTHER;
    }

    private String buildTextContext(String fileName, MimeType mimeType, byte[] data) {
        StringBuilder context = new StringBuilder();
        context.append("Attached file: ").append(StringUtils.hasText(fileName) ? fileName : "uploaded-file")
                .append("\nMIME type: ").append(mimeType);

        if (!TEXT_CONTEXT_TYPES.contains(mimeType)) {
            return context.append("\nContent: The file content is not text-readable in this chat context.")
                    .toString();
        }

        String text = new String(data, StandardCharsets.UTF_8);
        text = trimTextContext(text);

        return context.append("\nContent:\n").append(text).toString();
    }

    private String trimTextContext(String text) {
        String normalized = text.strip();
        if (normalized.length() <= MAX_TEXT_CONTEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TEXT_CONTEXT_LENGTH)
                + "\n\n[Attachment content truncated after %d characters.]".formatted(MAX_TEXT_CONTEXT_LENGTH);
    }

    private String sanitizeMediaName(String fileName) {
        String sanitized = (StringUtils.hasText(fileName) ? fileName : "uploaded-file")
                .replaceAll("[^A-Za-z0-9\\s\\-()\\[\\]]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        if (!StringUtils.hasText(sanitized)) {
            sanitized = "uploaded-file";
        }
        return sanitized.length() > MAX_MEDIA_NAME_LENGTH ? sanitized.substring(0, MAX_MEDIA_NAME_LENGTH) : sanitized;
    }

    public record ResolvedAttachmentInput(List<Media> media, String textContext) {
    }

    public enum AttachmentMediaKind {
        SPREADSHEET,
        TEXT_DOCUMENT,
        JSON,
        IMAGE,
        OTHER
    }
}
