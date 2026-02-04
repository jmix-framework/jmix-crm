package com.company.crm.view.client.charts.categories;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

public class ClientCategoryValueDescription {
    private final ClientCategoryInfo info;

    public ClientCategoryValueDescription(ClientCategoryInfo info) {
        this.info = info;
    }

    public String getCategory() {
        return info.category();
    }

    public Number getValue() {
        return info.value();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClientCategoryValueDescription) {
            return EqualsBuilder.reflectionEquals(this, obj);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(info);
    }
}
