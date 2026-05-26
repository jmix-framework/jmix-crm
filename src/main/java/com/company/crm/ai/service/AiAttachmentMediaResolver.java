package com.company.crm.ai.service;

import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.prompt.AiPromptContentBuilder;
import com.company.crm.ai.prompt.AiTextSanitizer;
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
import java.util.Map;
import java.util.Optional;

/**
 * Resolves AI conversation attachments into Spring AI {@link Media} objects for LLM input.
 */
@Service
public class AiAttachmentMediaResolver {

    private static final int MAX_TEXT_CONTEXT_LENGTH = 60_000;

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
        AiAttachmentMediaType mediaType = resolveSupportedMediaType(mimeTypeHint, fileName);

        if (mediaType.isImage()) {
            Media media = Media.builder()
                    .mimeType(mediaType.mimeType())
                    .data(data)
                    .name(AiMediaNameSanitizer.sanitize(fileName))
                    .build();
            return new ResolvedAttachmentInput(List.of(media), null);
        }

        return new ResolvedAttachmentInput(List.of(), buildTextContext(fileName, mediaType, data));
    }

    private byte[] readFileBytes(FileRef fileRef) {
        FileStorage fileStorage = fileStorageLocator.getByName(fileRef.getStorageName());
        try (InputStream inputStream = fileStorage.openStream(fileRef)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read attachment from file storage: " + fileRef.getFileName(), e);
        }
    }

    private AiAttachmentMediaType resolveSupportedMediaType(String rawMimeType, String fileName) {
        return tryParseMimeType(rawMimeType)
                .flatMap(AiAttachmentMediaType::fromMimeType)
                .or(() -> AiAttachmentMediaType.fromFileName(fileName))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported attachment media type for model input: " + fileName));
    }

    private Optional<MimeType> tryParseMimeType(String rawMimeType) {
        if (!StringUtils.hasText(rawMimeType)) {
            return Optional.empty();
        }
        try {
            return Optional.of(MimeTypeUtils.parseMimeType(rawMimeType));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String buildTextContext(String fileName, AiAttachmentMediaType mediaType, byte[] data) {
        return AiPromptContentBuilder.create()
                .appendFields(Map.of(
                        "Attached file", fileDisplayName(fileName),
                        "MIME type", mediaType.mimeType().toString()
                ))
                .appendParagraphIf(!mediaType.isTextContextReadable(), "Content: The file content is not text-readable in this chat context.")
                .appendSectionIf(mediaType.isTextContextReadable(), "Content", () -> trimTextContext(new String(data, StandardCharsets.UTF_8)))
                .build();
    }

    private String fileDisplayName(String fileName) {
        return StringUtils.hasText(fileName) ? fileName : "uploaded-file";
    }

    private String trimTextContext(String text) {
        String normalized = AiTextSanitizer.normalizeTextBlock(text, Integer.MAX_VALUE)
                .orElse("");
        if (normalized.length() <= MAX_TEXT_CONTEXT_LENGTH) {
            return normalized;
        }
        return AiTextSanitizer.truncate(normalized, MAX_TEXT_CONTEXT_LENGTH)
                + "\n\n[Attachment content truncated after %d characters.]".formatted(MAX_TEXT_CONTEXT_LENGTH);
    }

    public record ResolvedAttachmentInput(List<Media> media, String textContext) {
        public ResolvedAttachmentInput {
            media = media == null ? List.of() : List.copyOf(media);
        }
    }
}
