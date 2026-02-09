package com.company.crm.ai.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Table(name = "CHAT_MESSAGE", indexes = {
        @Index(name = "IDX_CHAT_MESSAGE_CONVERSATION", columnList = "CONVERSATION_ID"),
        @Index(name = "IDX_CHAT_MESSAGE", columnList = "CONVERSATION_ID, CREATED_DATE")
})
@Entity
public class ChatMessage {

    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @JoinColumn(name = "CONVERSATION_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AiConversation conversation;

    @Column(name = "CONTENT")
    @Lob
    private String content;

    @CreatedBy
    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private OffsetDateTime createdDate;

    @NotNull
    @Column(name = "TYPE_", nullable = false)
    private String type;

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