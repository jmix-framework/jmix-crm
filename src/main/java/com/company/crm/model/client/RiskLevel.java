package com.company.crm.model.client;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum RiskLevel implements EnumClass<String> {

    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW");

    private final String id;

    RiskLevel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static RiskLevel fromId(String id) {
        for (RiskLevel at : RiskLevel.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}