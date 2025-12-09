package com.company.crm.model.address;

import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@JmixEntity
@Embeddable
public class Address {

    @Column(name = "POSTAL_CODE")
    private String postalCode;

    @Column(name = "COUNTRY")
    private String country;

    @Column(name = "CITY")
    private String city;

    @Column(name = "STREET")
    private String street;

    @Column(name = "HOUSE")
    private String building;

    @Column(name = "APARTMENT")
    private String apartment;

    @InstanceName
    @DependsOnProperties({"country", "postalCode", "city", "street", "building", "apartment"})
    public String getInstanceName() {
        StringBuilder result = new StringBuilder();

        if (isNotBlank(country)) {
            result.append(country);
            appendSeparator(result);
        }

        if (isNotBlank(postalCode)) {
            result.append("(").append(postalCode).append(")");
            appendSeparator(result);
        }

        if (isNotBlank(city)) {
            result.append(city);
            appendSeparator(result);
        }

        if (isNotBlank(street)) {
            result.append(street);
            appendSeparator(result);
        }

        if (isNotBlank(building)) {
            result.append(building);
            appendSeparator(result);
        }

        if (isNotBlank(apartment)) {
            result.append(apartment);
        }

        return result.toString();
    }

    private void appendSeparator(StringBuilder builder) {
        final String separator = ", ";
        if (!builder.isEmpty()) {
            builder.append(separator);
        }
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getApartment() {
        return apartment;
    }

    public void setApartment(String apartment) {
        this.apartment = apartment;
    }
}