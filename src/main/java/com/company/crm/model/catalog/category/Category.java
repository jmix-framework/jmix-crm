package com.company.crm.model.catalog.category;

import com.company.crm.model.base.FullAuditEntity;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@JmixEntity
@Table(name = "CATEGORY", indexes = {
        @Index(name = "IDX_CATEGORY", columnList = "PARENT_ID")
})
public class Category extends FullAuditEntity {

    @InstanceName
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CODE", nullable = false, unique = true)
    private String code;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @JoinColumn(name = "PARENT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}