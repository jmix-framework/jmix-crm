package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Customer test entity with embedded address, comments, and associations
 */
@Entity
@Table(name = "TEST_CUSTOMER")
@JmixEntity
@Comment("Test customer entity for domain model export testing")
public class CustomerTestEntity extends BaseTestEntity {

    @Column(name = "NAME")
    @NotNull
    @Comment("Customer name - required field")
    private String name;

    @Column(name = "EMAIL")
    @Comment("Email address for customer communication")
    private String email;

    @Embedded
    @Comment("Embedded address information")
    private AddressTestEntity address;

    @OneToMany(mappedBy = "customer")
    @Comment("List of orders placed by this customer")
    private List<OrderTestEntity> orders;

    // Transient fields - should NOT be exported
    @Transient
    private String debugText;

    private transient String runtimeCache;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AddressTestEntity getAddress() {
        return address;
    }

    public void setAddress(AddressTestEntity address) {
        this.address = address;
    }

    public List<OrderTestEntity> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderTestEntity> orders) {
        this.orders = orders;
    }

    public String getDebugText() {
        return debugText;
    }

    public void setDebugText(String debugText) {
        this.debugText = debugText;
    }

    public String getRuntimeCache() {
        return runtimeCache;
    }

    public void setRuntimeCache(String runtimeCache) {
        this.runtimeCache = runtimeCache;
    }
}