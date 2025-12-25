package com.company.crm.model.client;

import com.company.crm.model.address.Address;
import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import io.jmix.core.entity.annotation.EmbeddedParameters;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@JmixEntity
@Table(name = "CLIENT", indexes = {
        @Index(name = "IDX_CLIENT_ACCOUNT_MANAGER", columnList = "ACCOUNT_MANAGER_ID")
})
public class Client extends FullAuditEntity {

    @InstanceName
    @Column(name = "NAME", nullable = false)
    private String name;

    @OrderBy("date")
    @OneToMany(mappedBy = "client")
    private List<Invoice> invoices;

    @OrderBy("date")
    @OneToMany(mappedBy = "client")
    private List<Order> orders;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Embedded
    @EmbeddedParameters(nullAllowed = false)
    private Address address;

    @Column(name = "TYPE_")
    private String type;

    @Column(name = "VAT_NUMBER")
    private String vatNumber;

    @Column(name = "REG_NUMBER")
    private String regNumber;

    @Column(name = "WEBSITE")
    private String website;

    @JoinColumn(name = "ACCOUNT_MANAGER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private User accountManager;

    @Composition
    @OrderBy("person")
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contact> contacts;

    @DependsOnProperties("invoices")
    public List<Payment> getPayments() {
        List<Payment> payments = new ArrayList<>();
        if (invoices == null) {
            return payments;
        }

        for (Invoice invoice : invoices) {
            payments.addAll(invoice.getPayments());
        }

        return payments;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public ClientType getType() {
        return ClientType.fromId(type);
    }

    public void setType(ClientType type) {
        this.type = type == null ? null : type.getId();
    }

    public User getAccountManager() {
        return accountManager;
    }

    public void setAccountManager(User accountManager) {
        this.accountManager = accountManager;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}