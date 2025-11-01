package com.company.crm.model.client;

import com.company.crm.model.base.VersionedEntity;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.user.User;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.List;

@JmixEntity
@Table(name = "CLIENT")
@Entity
public class Client extends VersionedEntity {

    @InstanceName
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "ADDRESS")
    private String address;

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
    @OneToMany(mappedBy = "client")
    private List<Contact> contacts;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
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