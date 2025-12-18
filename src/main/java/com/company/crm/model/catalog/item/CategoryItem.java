package com.company.crm.model.catalog.item;

import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.catalog.category.Category;
import io.jmix.core.FileRef;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Entity
@JmixEntity
@Table(name = "CATEGORY_ITEM", indexes = {
        @Index(name = "IDX_CATEGORY_ITEM_CATEGORY", columnList = "CATEGORY_ID")
})
public class CategoryItem extends FullAuditEntity {

    @InstanceName
    @Column(name = "NAME", nullable = false)
    private String name;

    @JoinColumn(name = "CATEGORY_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @Column(name = "IMAGE", length = 1024)
    private FileRef image;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CODE", nullable = false, unique = true)
    private String code;

    @Column(name = "UOM")
    private String uom;

    @Composition
    @OneToMany(mappedBy = "categoryItem")
    private List<CategoryItemComment> comments;

    public FileRef getImage() {
        return image;
    }

    public void setImage(FileRef image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CategoryItemComment> getComments() {
        return comments;
    }

    public void setComments(List<CategoryItemComment> comments) {
        this.comments = comments;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}