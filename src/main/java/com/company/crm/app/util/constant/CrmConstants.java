package com.company.crm.app.util.constant;

public final class CrmConstants {

    public static class SpringProfiles {
        public static final String DEV = "dev";
        public static final String PROD = "prod";
        public static final String TEST = "test";

        private SpringProfiles() {
        }
    }

    public static class ViewIds {

        public static final String MAIN = "MainView";
        public static final String LOGIN = "LoginView";

        public static final String CATEGORY_LIST = "Category.list";
        public static final String CATEGORY_DETAIL = "Category.detail";

        public static final String CATEGORY_ITEM_LIST = "CategoryItem.list";
        public static final String CATEGORY_ITEM_DETAIL = "CategoryItem.detail";

        public static final String CLIENT_LIST = "Client.list";
        public static final String CLIENT_DETAIL = "Client.detail";

        public static final String FINANCE = "FinanceView";

        public static final String HOME = "HomeView";

        public static final String INVOICE_LIST = "Invoice.list";
        public static final String INVOICE_DETAIL = "Invoice.detail";

        public static final String ORDER_LIST = "Order.list";
        public static final String ORDER_DETAIL = "Order.detail";

        public static final String ORDER_ITEM_LIST = "OrderItem.list";
        public static final String ORDER_ITEM_DETAIL = "OrderItem.detail";

        public static final String PAYMENT_LIST = "Payment.list";
        public static final String PAYMENT_DETAIL = "Payment.detail";

        public static final String USER_LIST = "User.list";
        public static final String USER_DETAIL = "User.detail";

        public static final String USER_TASK_LIST = "UserTask.list";

        private ViewIds() {
        }
    }

    private CrmConstants() {
    }
}
