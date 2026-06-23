package com.company.crm.ai.model;

import com.company.crm.model.base.CreateAuditEntity;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<ChatMessage> getSortedMessages() {
        return Optional.ofNullable(messages)
                .orElse(List.of())
                .stream()
                .sorted(Comparator
                        .comparing(ChatMessage::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ChatMessage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
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
