package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Order item test entity - child in composition relationship
 */
@Entity
@Table(name = "TEST_ORDER_ITEM")
@JmixEntity
public class OrderItemTestEntity extends BaseTestEntity {

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(name = "UNIT_PRICE", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private OrderTestEntity order;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public OrderTestEntity getOrder() {
        return order;
    }

    public void setOrder(OrderTestEntity order) {
        this.order = order;
    }
}