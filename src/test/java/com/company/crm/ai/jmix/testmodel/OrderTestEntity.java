package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Order test entity with composition relationship and required association
 */
@Entity
@Table(name = "TEST_ORDER")
@JmixEntity
@Comment("Test order entity for testing associations and composition")
public class OrderTestEntity extends BaseTestEntity {

    @Column(name = "ORDER_DATE")
    @Comment("Date when the order was placed")
    private LocalDate orderDate;

    @Column(name = "ORDER_NUMBER")
    @Comment("Unique order number for reference")
    private String orderNumber;

    @Column(name = "STATUS")
    @Comment("Current status of the order")
    private TestOrderStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    @Comment("Customer who placed this order - required")
    private CustomerTestEntity customer;

    @Composition
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Comment("Order line items - composition relationship")
    private List<OrderItemTestEntity> items;

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public TestOrderStatus getStatus() {
        return status;
    }

    public void setStatus(TestOrderStatus status) {
        this.status = status;
    }

    public CustomerTestEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerTestEntity customer) {
        this.customer = customer;
    }

    public List<OrderItemTestEntity> getItems() {
        return items;
    }

    public void setItems(List<OrderItemTestEntity> items) {
        this.items = items;
    }
}