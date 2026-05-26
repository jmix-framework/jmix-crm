package com.company.crm.ai.service;

import io.jmix.core.FileRef;
import org.springframework.util.StringUtils;

public record PendingAttachmentInput(FileRef fileRef, String fileName) {

    private static final String FALLBACK_FILE_NAME = "uploaded-file";

    public String resolvedFileName() {
        if (StringUtils.hasText(fileName)) {
            return fileName;
        }
        if (fileRef != null && StringUtils.hasText(fileRef.getFileName())) {
            return fileRef.getFileName();
        }
        return FALLBACK_FILE_NAME;
    }
}
