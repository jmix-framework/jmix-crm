package com.company.crm.ai.entity;

import io.jmix.core.FileRef;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.PropertyDatatype;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Table(name = "AI_CONVERSATION_ATTACHMENT", indexes = {
        @Index(name = "IDX_AI_CONV_ATTACH_CONV", columnList = "CONVERSATION_ID")
})
@Entity
public class AiConversationAttachment {

    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @NotNull
    @JoinColumn(name = "CONVERSATION_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AiConversation conversation;

    @NotNull
    @Column(name = "FILE_", nullable = false, length = 1024)
    @PropertyDatatype("fileRef")
    private FileRef file;

    @NotNull
    @Column(name = "FILE_NAME", nullable = false)
    private String fileName;

    @Column(name = "TITLE")
    private String title;

    @NotNull
    @Column(name = "TYPE_", nullable = false)
    private String type;

    @CreatedBy
    @Column(name = "CREATED_BY")
    private String createdBy;

    @CreatedDate
    @Column(name = "CREATED_DATE")
    private OffsetDateTime createdDate;

    public AiAttachmentType getType() {
        return type == null ? null : AiAttachmentType.fromId(type);
    }

    public void setType(AiAttachmentType type) {
        this.type = type == null ? null : type.getId();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public FileRef getFile() {
        return file;
    }

    public void setFile(FileRef file) {
        this.file = file;
    }

    public AiConversation getConversation() {
        return conversation;
    }

    public void setConversation(AiConversation conversation) {
        this.conversation = conversation;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
