package com.company.crm.ai.model;

import com.company.crm.model.base.CreateAuditEntity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.Composition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JmixEntity
@Table(name = "CHAT_MESSAGE", indexes = {
        @Index(name = "IDX_CHAT_MESSAGE_CONVERSATION", columnList = "CONVERSATION_ID"),
        @Index(name = "IDX_CHAT_MESSAGE", columnList = "CONVERSATION_ID, CREATED_DATE")
})
@Entity
public class ChatMessage extends CreateAuditEntity {

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "CONVERSATION_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AiConversation conversation;

    @InstanceName
    @Column(name = "CONTENT")
    @Lob
    private String content;

    @NotNull
    @Column(name = "TYPE_", nullable = false)
    private String type;

    @Composition
    @OneToMany(mappedBy = "message")
    @OrderBy("sortOrder ASC, createdDate ASC, id ASC")
    private List<ChatMessageEntityReference> entityReferences;

    @Composition
    @OneToMany(mappedBy = "message")
    @OrderBy("createdDate ASC, id ASC")
    private List<AiConversationAttachment> attachments;

    public List<AiConversationAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AiConversationAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<ChatMessageEntityReference> getEntityReferences() {
        return entityReferences;
    }

    public void setEntityReferences(List<ChatMessageEntityReference> entityReferences) {
        this.entityReferences = entityReferences;
    }

    public ChatMessageType getType() {
        return type == null ? null : ChatMessageType.fromId(type);
    }

    public void setType(ChatMessageType type) {
        this.type = type == null ? null : type.getId();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public AiConversation getConversation() {
        return conversation;
    }

    public void setConversation(AiConversation conversation) {
        this.conversation = conversation;
    }

}
