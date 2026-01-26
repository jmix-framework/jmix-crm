package com.company.crm.model.invoice;

import com.company.crm.app.service.finance.InvoiceService;
import com.company.crm.app.util.context.AppContext;
import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import io.jmix.core.DeletePolicy;
import io.jmix.core.Messages;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.PropertyDatatype;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@JmixEntity
@Table(name = "INVOICE", indexes = {
        @Index(name = "IDX_INVOICE_ORDER", columnList = "ORDER_ID"),
        @Index(name = "IDX_INVOICE_CLIENT", columnList = "CLIENT_ID")
})
public class Invoice extends FullAuditEntity {

    @Column(name = "NUMBER", nullable = false, unique = true)
    private String number;

    @JoinColumn(name = "CLIENT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    @OrderBy("date DESC")
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "invoice")
    private List<Payment> payments;

    @JoinColumn(name = "ORDER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    @Column(name = "DATE_")
    private LocalDate date;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @PositiveOrZero
    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "SUBTOTAL")
    private BigDecimal subtotal;

    @PositiveOrZero
    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "VAT")
    private BigDecimal vat;

    @PositiveOrZero
    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "TOTAL")
    private BigDecimal total;

    @Column(name = "STATUS")
    private Integer status;

    @InstanceName
    @DependsOnProperties({"number", "date"})
    public String getInstanceName(DatatypeFormatter datatypeFormatter, Messages messages) {
        if (StringUtils.isNotBlank(number)) {
            return String.format("%s from %s", number, datatypeFormatter.formatLocalDate(date));
        } else {
            return messages.getMessage("newInvoice");
        }
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public InvoiceStatus getStatus() {
        return InvoiceStatus.fromId(status);
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public BigDecimal getTotal() {
        return total != null ? total : BigDecimal.ZERO;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public void setVat(BigDecimal vat) {
        this.vat = vat;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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
        return AppContext.getBean(InvoiceService.class).getNextInvoiceNumber();
    }
}