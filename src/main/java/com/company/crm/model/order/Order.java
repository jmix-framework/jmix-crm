package com.company.crm.model.order;

import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.util.context.AppContext;
import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PercentDataType;
import com.company.crm.model.datatype.PriceDataType;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import io.jmix.core.metamodel.annotation.PropertyDatatype;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity(name = "Order_")
@JmixEntity
@Table(name = "ORDER_", indexes = {
        @Index(name = "IDX_ORDER__CLIENT", columnList = "CLIENT_ID")
})
public class Order extends FullAuditEntity {

    @Column(name = "NUMBER", nullable = false, unique = true)
    private String number;

    @JoinColumn(name = "CLIENT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @Column(name = "DATE_")
    private LocalDate date;

    @Column(name = "QUOTE")
    private String quote;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "TOTAL")
    private BigDecimal total;

    // TODO: OVERALL_DISCOUNT_VALUE?
    @Column(name = "DISCOUNT_VALUE", precision = 19, scale = 2)
    private BigDecimal discountValue;

    // TODO: OVERALL_DISCOUNT_PERCENT?
    @Min(0)
    @Max(100)
    @PropertyDatatype(PercentDataType.NAME)
    @Column(name = "DISCOUNT_PERCENT", precision = 19, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "STATUS")
    private Integer status;

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public OrderStatus getStatus() {
        return OrderStatus.fromId(status);
    }

    public void setStatus(OrderStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent == null ? BigDecimal.ZERO : discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getDiscountValue() {
        return discountValue == null ? BigDecimal.ZERO : discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getTotal() {
        return total == null ? BigDecimal.ZERO : total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @JmixProperty
    @DependsOnProperties("orderItems")
    public BigDecimal getItemsTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem orderItem : getOrderItems()) {
            total = total.add(orderItem.getTotal());
        }
        return total;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @InstanceName
    @DependsOnProperties({"number", "date", "total"})
    public String getInstanceName(DatatypeFormatter datatypeFormatter) {
        String orderNumber = getNumber();
        if (StringUtils.isNotBlank(orderNumber)) {
            return String.format("Order %s from %s", orderNumber, datatypeFormatter.formatLocalDate(date));
        } else {
            return "New Order";
        }
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @PrePersist
    public void prePersist() {
        setNumber(generateNextOrderNumber());
    }

    private String generateNextOrderNumber() {
        return AppContext.getBean(OrderService.class).getNextOrderNumber();
    }
}