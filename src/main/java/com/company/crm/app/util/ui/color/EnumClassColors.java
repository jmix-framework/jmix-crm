package com.company.crm.app.util.ui.color;

import com.company.crm.model.client.ClientType;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.OrderStatus;
import com.vaadin.flow.theme.lumo.LumoUtility;

import static com.company.crm.app.util.ui.CrmUiUtils.CONTRAST_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.DEFAULT_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.ERROR_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.SUCCESS_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.WARNING_BADGE;

public final class EnumClassColors {

    public static String getBackgroundClass(OrderStatus status) {
        return switch (status) {
            case NEW -> LumoUtility.Background.CONTRAST_10;
            case ACCEPTED -> LumoUtility.Background.PRIMARY;
            case IN_PROGRESS -> LumoUtility.Background.WARNING;
            case DONE -> LumoUtility.Background.SUCCESS;
        };
    }

    public static String getBadgeVariant(OrderStatus status) {
        return switch (status) {
            case NEW -> CONTRAST_BADGE;
            case ACCEPTED -> DEFAULT_BADGE;
            case IN_PROGRESS -> WARNING_BADGE;
            case DONE -> SUCCESS_BADGE;
        };
    }

    public static String getBadgeVariant(InvoiceStatus status) {
        return switch (status) {
            case NEW -> DEFAULT_BADGE;
            case PENDING -> WARNING_BADGE;
            case OVERDUE -> ERROR_BADGE;
            case PAID -> SUCCESS_BADGE;
        };
    }

    public static String getBadgeVariant(ClientType status) {
        return switch (status) {
            case BUSINESS -> SUCCESS_BADGE;
            case INDIVIDUAL -> WARNING_BADGE;
        };
    }

    private EnumClassColors() {
    }
}
