package com.company.crm.ai.service;

import com.company.crm.ai.model.AiConversationAttachment;
import io.jmix.core.DataManager;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves AI conversation attachments into Spring AI {@link Media} objects for LLM input.
 */
@Service
public class AiAttachmentMediaResolver {

    private static final int MAX_MEDIA_NAME_LENGTH = 96;

    private static final Set<MimeType> SUPPORTED_MEDIA_TYPES = Set.of(
            Media.Format.DOC_PDF,
            Media.Format.DOC_CSV,
            Media.Format.DOC_DOC,
            Media.Format.DOC_DOCX,
            Media.Format.DOC_XLS,
            Media.Format.DOC_XLSX,
            Media.Format.DOC_HTML,
            Media.Format.DOC_TXT,
            Media.Format.DOC_MD,
            Media.Format.IMAGE_PNG,
            Media.Format.IMAGE_JPEG,
            Media.Format.IMAGE_GIF,
            Media.Format.IMAGE_WEBP
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
            Map.entry(".png", Media.Format.IMAGE_PNG),
            Map.entry(".jpg", Media.Format.IMAGE_JPEG),
            Map.entry(".jpeg", Media.Format.IMAGE_JPEG),
            Map.entry(".gif", Media.Format.IMAGE_GIF),
            Map.entry(".webp", Media.Format.IMAGE_WEBP)
    );

    private final DataManager dataManager;
    private final FileStorageLocator fileStorageLocator;

    public AiAttachmentMediaResolver(DataManager dataManager, FileStorageLocator fileStorageLocator) {
        this.dataManager = dataManager;
        this.fileStorageLocator = fileStorageLocator;
    }

    /**
     * Resolves attachments for a conversation into Spring AI Media objects.
     */
    public List<Media> resolve(UUID conversationId, List<AttachmentRef> attachmentRefs) {
        if (attachmentRefs.isEmpty()) {
            return List.of();
        }
        if (conversationId == null) {
            throw new IllegalArgumentException("Attachment media input requires a valid conversation UUID.");
        }

        return attachmentRefs.stream()
                .map(ref -> resolveAttachment(conversationId, ref))
                .toList();
    }

    private Media resolveAttachment(UUID conversationId, AttachmentRef ref) {
        AiConversationAttachment attachment = dataManager.load(AiConversationAttachment.class)
                .query("select e from AiConversationAttachment e where e.id = :id and e.conversation.id = :conversationId")
                .parameter("id", ref.attachmentId())
                .parameter("conversationId", conversationId)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attachment not found in conversation: " + ref.attachmentId()));

        FileRef fileRef = attachment.getFile();
        if (fileRef == null) {
            throw new IllegalArgumentException("Attachment has no file payload: " + ref.attachmentId());
        }

        return Media.builder()
                .mimeType(resolveSupportedMimeType(ref.mimeType(), attachment.getFileName()))
                .data(readFileBytes(fileRef))
                .name(sanitizeMediaName(attachment.getFileName()))
                .build();
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
        if (parsed != null && SUPPORTED_MEDIA_TYPES.contains(parsed)) {
            return parsed;
        }

        MimeType fromExtension = mimeTypeFromExtension(fileName);
        if (fromExtension != null && SUPPORTED_MEDIA_TYPES.contains(fromExtension)) {
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

    private MimeType mimeTypeFromExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        String normalized = fileName.toLowerCase();
        return EXTENSION_MIME_TYPES.entrySet().stream()
                .filter(entry -> normalized.endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
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

    public record AttachmentRef(UUID attachmentId, String mimeType) {
    }
}
