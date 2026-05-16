package com.company.crm.ai.service;

import io.jmix.core.FileRef;

public record PendingAttachmentInput(FileRef fileRef, String fileName) {
}
