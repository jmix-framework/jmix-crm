package com.company.crm.ai.model;

import com.company.crm.model.base.CreateAuditEntity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@JmixEntity
@Table(name = "CHAT_MESSAGE_ENTITY_REFERENCE", indexes = {
        @Index(name = "IDX_CHAT_MSG_ENTITY_REF_MESSAGE", columnList = "MESSAGE_ID")
})
@Entity
public class ChatMessageEntityReference extends CreateAuditEntity {

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "MESSAGE_ID", nullable = false)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ChatMessage message;

    @InstanceName
    @NotNull
    @Column(name = "ENTITY_REFERENCE", nullable = false)
    private String entityReference;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getEntityReference() {
        return entityReference;
    }

    public void setEntityReference(String entityReference) {
        this.entityReference = entityReference;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }
}
