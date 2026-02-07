package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Embeddable;

/**
 * Embeddable test entity for testing @Embedded support
 */
@Embeddable
@JmixEntity
public class AddressTestEntity {

    private String street;
    private String zip;
    private String city;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}