package com.company.crm.model.order;

import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.datatype.PriceDataType;
import io.jmix.core.DeletePolicy;
import io.jmix.core.MetadataTools;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import io.jmix.core.metamodel.annotation.PropertyDatatype;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@JmixEntity
@Table(name = "ORDER_ITEM", indexes = {
        @Index(name = "IDX_ORDER_ITEM_CATEGORY_ITEM", columnList = "CATEGORY_ITEM_ID"),
        @Index(name = "IDX_ORDER_ITEM_ORDER", columnList = "ORDER_ID")
})
@Entity
public class OrderItem extends FullAuditEntity {

    @OnDeleteInverse(DeletePolicy.UNLINK)
    @JoinColumn(name = "CATEGORY_ITEM_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryItem categoryItem;

    @Column(name = "QUANTITY", precision = 19, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "DISCOUNT", precision = 19, scale = 2)
    private BigDecimal discount;

    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "NET_PRICE", nullable = false)
    private BigDecimal netPrice;

    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "GROSS_PRICE", nullable = false)
    private BigDecimal grossPrice;

    @Column(name = "VAT_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "VAT_INCLUDED")
    private Boolean vatIncluded;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    @JmixProperty
    public BigDecimal getTotal() {
        quantity = quantity == null ? BigDecimal.ZERO : quantity;
        return netPrice.multiply(quantity).subtract(discount);
    }

    public Boolean getVatIncluded() {
        return vatIncluded;
    }

    public void setVatIncluded(Boolean vatIncluded) {
        this.vatIncluded = vatIncluded;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }

    public BigDecimal getGrossPrice() {
        return grossPrice;
    }

    public void setGrossPrice(BigDecimal grossPrice) {
        this.grossPrice = grossPrice;
    }

    public BigDecimal getNetPrice() {
        return netPrice;
    }

    public void setNetPrice(BigDecimal netPrice) {
        this.netPrice = netPrice;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public CategoryItem getCategoryItem() {
        return categoryItem;
    }

    public void setCategoryItem(CategoryItem categoryItem) {
        this.categoryItem = categoryItem;
    }

    @InstanceName
    @DependsOnProperties({"categoryItem", "quantity"})
    public String getInstanceName(MetadataTools metadataTools, DatatypeFormatter datatypeFormatter) {
        return String.format("%s %s",
                metadataTools.format(categoryItem),
                datatypeFormatter.formatBigDecimal(quantity));
    }
}