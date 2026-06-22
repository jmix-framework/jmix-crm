package com.company.crm.app.util.ui.color;

import com.company.crm.app.util.ui.theme.CrmStyleUtility;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.OrderStatus;

import static com.company.crm.app.util.ui.CrmUiUtils.CONTRAST_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.DEFAULT_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.ERROR_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.SUCCESS_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.WARNING_BADGE;

public final class EnumClassColors {

    public static String getBackgroundClass(OrderStatus status) {
        return switch (status) {
            case NEW -> CrmStyleUtility.Background.ERROR_10;
            case ACCEPTED -> CrmStyleUtility.Background.PRIMARY;
            case IN_PROGRESS -> CrmStyleUtility.Background.WARNING;
            case DONE -> CrmStyleUtility.Background.SUCCESS;
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
            case NEW -> CONTRAST_BADGE;
            case PENDING -> DEFAULT_BADGE;
            case OVERDUE -> ERROR_BADGE;
            case PAID -> SUCCESS_BADGE;
        };
    }

    public static String getBadgeVariant(ClientType status) {
        return switch (status) {
            case BUSINESS -> SUCCESS_BADGE;
            case INDIVIDUAL -> DEFAULT_BADGE;
        };
    }

    private EnumClassColors() {
    }
}
