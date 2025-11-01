package com.company.crm.model.catalog.item;

import com.company.crm.model.base.CreateAuditEntity;
import com.company.crm.model.user.User;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@JmixEntity
@Table(name = "CATEGORY_ITEM_COMMENT", indexes = {
        @Index(name = "IDX_CATEGORY_ITEM_COMMENT_CATEGORY_ITEM", columnList = "CATEGORY_ITEM_ID"),
        @Index(name = "IDX_CATEGORY_ITEM_COMMENT_SENDER", columnList = "SENDER_ID")
})
@Entity
public class CategoryItemComment extends CreateAuditEntity {
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "CATEGORY_ITEM_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CategoryItem categoryItem;

    @Lob
    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @OnDeleteInverse(DeletePolicy.UNLINK)
    @JoinColumn(name = "SENDER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private User sender;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public CategoryItem getCategoryItem() {
        return categoryItem;
    }

    public void setCategoryItem(CategoryItem categoryItem) {
        this.categoryItem = categoryItem;
    }

}