package com.company.crm.model.order;

import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.service.util.UniqueNumbersService;
import com.company.crm.app.util.context.AppContext;
import com.company.crm.app.util.price.PriceCalculator;
import com.company.crm.model.HasUniqueNumber;
import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import io.jmix.core.DeletePolicy;
import io.jmix.core.Messages;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.company.crm.app.util.price.PriceCalculator.calculateSubtotal;

@Entity(name = "Order_")
@JmixEntity
@Table(name = "ORDER_", indexes = {
        @Index(name = "IDX_ORDER__CLIENT", columnList = "CLIENT_ID")
})
public class Order extends FullAuditEntity implements HasUniqueNumber {

    @Column(name = "NUMBER", nullable = false, unique = true)
    private String number;

    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "order")
    private List<Invoice> invoices;

    @JoinColumn(name = "CLIENT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    @Composition
    @OrderBy("createdDate DESC")
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @Column(name = "DATE_")
    private LocalDate date;

    @Column(name = "PURCHASE_ORDER")
    private String purchaseOrder;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    @PositiveOrZero
    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "TOTAL")
    private BigDecimal total;

    @PositiveOrZero
    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "DISCOUNT_VALUE")
    private BigDecimal discountValue;

    @Min(0)
    @Max(100)
    @PropertyDatatype("percent")
    @Column(name = "DISCOUNT_PERCENT")
    private BigDecimal discountPercent;

    @Column(name = "STATUS")
    private Integer status;

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }

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
        return total == null ? PriceCalculator.calculateTotal(this) : total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @JmixProperty
    @DependsOnProperties({"orderItems"})
    @PropertyDatatype(PriceDataType.NAME)
    public BigDecimal getSubTotal() {
        return calculateSubtotal(this);
    }

    @JmixProperty
    @DependsOnProperties({"orderItems"})
    @PropertyDatatype(PriceDataType.NAME)
    public BigDecimal getVat() {
        return PriceCalculator.calculateVat(this);
    }

    @JmixProperty
    @DependsOnProperties({"orderItems"})
    @PropertyDatatype(PriceDataType.NAME)
    public BigDecimal getItemsTotal() {
        BigDecimal total = BigDecimal.ZERO;

        if (orderItems == null) {
            return total;
        }

        for (OrderItem orderItem : orderItems) {
            total = total.add(orderItem.getTotal());
        }

        return total;
    }

    /// @see OrderService#getLeftOverSum
    @JmixProperty
    @PropertyDatatype(PriceDataType.NAME)
    public BigDecimal getLeftOverSum() {
        return getOrderService().getLeftOverSum(this);
    }

    ///  @see OrderService#getPaid
    @JmixProperty
    @PropertyDatatype(PriceDataType.NAME)
    public BigDecimal getPaid() {
        return getOrderService().getPaid(this);
    }

    /// invoiced = sum of {@link Invoice#getSubtotal()}
    @JmixProperty
    @DependsOnProperties({"invoices"})
    @PropertyDatatype(PriceDataType.NAME)
    public BigDecimal getInvoiced() {
        List<Invoice> invoices = getInvoices();
        if (invoices == null || invoices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return invoices.stream()
                .map(Invoice::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @InstanceName
    @DependsOnProperties({"number", "date", "total"})
    public String getInstanceName(DatatypeFormatter datatypeFormatter, Messages messages) {
        if (StringUtils.isNotBlank(number)) {
            return String.format("%s from %s", number, datatypeFormatter.formatLocalDate(date));
        } else {
            return messages.getMessage("newOrder");
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(String purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
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

    public String getNumber() {
        return number == null ? getNumberWillBeGeneratedMessage() : number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @PrePersist
    public void prePersist() {
        setNumber(generateNextNumber());
        setPurchaseOrder(generateNextPurchaseOrderNumber());
    }

    private static String generateNextPurchaseOrderNumber() {
        return AppContext.getBean(UniqueNumbersService.class).getNextPurchaseOrderNumber();
    }

    private OrderService getOrderService() {
        return AppContext.getBean(OrderService.class);
    }
}