package com.company.crm.model.payment;

import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import io.jmix.core.metamodel.annotation.PropertyDatatype;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@JmixEntity
@Table(name = "PAYMENT", indexes = {
        @Index(name = "IDX_PAYMENT_INVOICE", columnList = "INVOICE_ID")
})
public class Payment extends FullAuditEntity {

    @JoinColumn(name = "INVOICE_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Invoice invoice;

    @Column(name = "DATE_")
    private LocalDate date;

    @PropertyDatatype(PriceDataType.NAME)
    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @JmixProperty
    @DependsOnProperties("invoice")
    public Order getOrder() {
        if (invoice == null) {
            return null;
        }
        return invoice.getOrder();
    }

    @JmixProperty
    @DependsOnProperties("invoice")
    public Client getClient() {
        if (invoice == null) {
            return null;
        }
        return invoice.getClient();
    }

    @InstanceName
    public String getInstanceName() {
        return String.format("Payment %s from %s",
                PriceDataType.defaultFormat(amount), date);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

}