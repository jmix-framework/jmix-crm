package com.company.crm.model.user;

import com.company.crm.model.base.VersionedEntity;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@JmixEntity
@Table(name = "USER_TASK", indexes = {
        @Index(name = "IDX_USER_TASK_AUTHOR", columnList = "AUTHOR_ID")
})
@Entity
public class UserTask extends VersionedEntity {

    @Column(name = "IS_COMPLETED")
    private Boolean isCompleted;

    @InstanceName
    @Column(name = "TITLE", nullable = false)
    private String title;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @JoinColumn(name = "AUTHOR_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}
