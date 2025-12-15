package com.company.crm.app.util.ui.color;

import com.company.crm.model.client.ClientType;
import com.company.crm.model.order.OrderStatus;
import com.vaadin.flow.theme.lumo.LumoUtility;

public final class StatusColors {

    public static String getBackgroundClass(OrderStatus status) {
        return switch (status) {
            case NEW -> LumoUtility.Background.CONTRAST_20;
            case ACCEPTED -> LumoUtility.Background.PRIMARY_50;
            case IN_PROGRESS -> LumoUtility.Background.WARNING;
            case DONE -> LumoUtility.Background.SUCCESS_50;
        };
    }

    public static String getBadgeVariant(OrderStatus status) {
        return switch (status) {
            case NEW -> "contrast";
            case ACCEPTED -> "default";
            case IN_PROGRESS -> "warning";
            case DONE -> "success";
        };
    }

    public static String getBadgeVariant(ClientType status) {
        return switch (status) {
            case BUSINESS -> "success";
            case INDIVIDUAL -> "warning";
        };
    }

    private StatusColors() {
    }
}
