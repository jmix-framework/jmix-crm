package com.company.crm.ai.model;

import com.company.crm.model.base.CreateAuditEntity;
import com.company.crm.model.base.FullAuditEntity;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "AI_CONVERSATION")
@Entity
public class AiConversation extends CreateAuditEntity {

    @Column(name = "TITLE")
    private String title;

    @Column(name = "FIRST_MESSAGE_SENT", nullable = false)
    private Boolean firstMessageSent = false;

    @Composition
    @OneToMany(mappedBy = "conversation")
    @OrderBy("createdDate ASC")
    private List<ChatMessage> messages;

    @Composition
    @OneToMany(mappedBy = "conversation")
    @OrderBy("createdDate ASC")
    private List<AiConversationAttachment> attachments;

    public List<AiConversationAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AiConversationAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @InstanceName
    @DependsOnProperties({"title", "id"})
    public String getInstanceName() {
        return title != null ? title : "Conversation " + getId();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getFirstMessageSent() {
        return firstMessageSent;
    }

    public void setFirstMessageSent(Boolean firstMessageSent) {
        this.firstMessageSent = firstMessageSent;
    }

}
