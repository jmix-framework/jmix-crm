package com.company.crm.ai.model;

import com.company.crm.model.base.CreateAuditEntity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.FileRef;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.PropertyDatatype;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@JmixEntity
@Table(name = "AI_CONVERSATION_ATTACHMENT", indexes = {
        @Index(name = "IDX_AI_CONV_ATTACH_MESSAGE", columnList = "MESSAGE_ID")
})
@Entity
public class AiConversationAttachment extends CreateAuditEntity {

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @NotNull
    @JoinColumn(name = "MESSAGE_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ChatMessage message;

    @NotNull
    @Column(name = "FILE_", nullable = false, length = 1024)
    @PropertyDatatype("fileRef")
    private FileRef file;

    @InstanceName
    @NotNull
    @Column(name = "FILE_NAME", nullable = false)
    private String fileName;

    @Column(name = "TITLE")
    private String title;

    @NotNull
    @Column(name = "ORIGIN", nullable = false)
    private String origin;

    public AiAttachmentOrigin getOrigin() {
        return origin == null ? null : AiAttachmentOrigin.fromId(origin);
    }

    public void setOrigin(AiAttachmentOrigin origin) {
        this.origin = origin == null ? null : origin.getId();
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

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

}
