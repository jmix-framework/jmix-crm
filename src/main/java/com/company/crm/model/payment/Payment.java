package com.company.crm.model.payment;

import com.company.crm.model.HasUniqueNumber;
import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import io.jmix.core.Messages;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@JmixEntity
@Table(name = "PAYMENT", indexes = {
        @Index(name = "IDX_PAYMENT_INVOICE", columnList = "INVOICE_ID")
})
public class Payment extends FullAuditEntity implements HasUniqueNumber {

    @Column(name = "NUMBER", nullable = false, unique = true)
    private String number;

    @JoinColumn(name = "INVOICE_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Invoice invoice;

    @Column(name = "DATE_")
    private LocalDate date;

    @PositiveOrZero
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
    @DependsOnProperties({"number", "date"})
    public String getInstanceName(DatatypeFormatter datatypeFormatter, Messages messages) {
        if (StringUtils.isNotBlank(number)) {
            return String.format("%s from %s", number, datatypeFormatter.formatLocalDate(date));
        } else {
            return messages.getMessage("newPayment");
        }
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @PrePersist
    public void prePersist() {
        setNumber(generateNextNumber());
    }
}