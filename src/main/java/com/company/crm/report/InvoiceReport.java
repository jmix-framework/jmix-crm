package com.company.crm.report;

import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import com.company.crm.security.AdministratorRole;
import com.company.crm.security.ManagerRole;
import com.company.crm.security.SupervisorRole;
import com.company.crm.view.invoice.InvoiceDetailView;
import com.company.crm.view.invoice.InvoiceListView;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import io.jmix.reports.annotation.AvailableForRoles;
import io.jmix.reports.annotation.AvailableInViews;
import io.jmix.reports.annotation.BandDef;
import io.jmix.reports.annotation.DataSetDef;
import io.jmix.reports.annotation.DataSetDelegate;
import io.jmix.reports.annotation.EntityParameterDef;
import io.jmix.reports.annotation.InputParameterDef;
import io.jmix.reports.annotation.ReportDef;
import io.jmix.reports.annotation.TemplateDef;
import io.jmix.reports.entity.DataSetType;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.yarg.loaders.ReportDataLoader;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReportDef(
        code = InvoiceReport.CODE,
        uuid = InvoiceReport.ID,
        name = InvoiceReport.REPORT_NAME
)
@AvailableForRoles(roleClasses = {AdministratorRole.class, SupervisorRole.class, ManagerRole.class})
@AvailableInViews(viewClasses = {InvoiceDetailView.class, InvoiceListView.class})
@TemplateDef(
        isDefault = true,
        code = InvoiceReport.TEMPLATE_CODE,
        filePath = InvoiceReport.TEMPLATE_PATH,
        outputType = ReportOutputType.PDF,
        outputNamePattern = InvoiceReport.OUTPUT_NAME_PATTERN
)
@InputParameterDef(
        alias = InvoiceReport.PARAM_INVOICE,
        name = InvoiceReport.PARAM_INVOICE_NAME,
        type = ParameterType.ENTITY,
        required = true,
        entity = @EntityParameterDef(entityClass = Invoice.class)
)
@BandDef(
        name = InvoiceReport.BAND_ROOT,
        root = true
)
@BandDef(
        name = InvoiceReport.BAND_INVOICE,
        parent = InvoiceReport.BAND_ROOT,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_INVOICE,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_ORDER,
        parent = InvoiceReport.BAND_INVOICE,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_ORDER,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_CLIENT,
        parent = InvoiceReport.BAND_INVOICE,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_CLIENT,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_PAYMENT,
        parent = InvoiceReport.BAND_INVOICE,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_PAYMENT,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_PRICE,
        parent = InvoiceReport.BAND_ROOT,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_PRICE,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_QUANTITY,
        parent = InvoiceReport.BAND_ROOT,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_QUANTITY,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_DISCOUNT,
        parent = InvoiceReport.BAND_ROOT,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_DISCOUNT,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_VAT,
        parent = InvoiceReport.BAND_ROOT,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_VAT,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_TOTAL,
        parent = InvoiceReport.BAND_ROOT,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_TOTAL,
                type = DataSetType.DELEGATE
        )
)
@BandDef(
        name = InvoiceReport.BAND_AMOUNT,
        parent = InvoiceReport.BAND_ROOT,
        dataSets = @DataSetDef(
                name = InvoiceReport.BAND_AMOUNT,
                type = DataSetType.DELEGATE
        )
)
public class InvoiceReport {

    public static final String CODE = "invoice-report";
    public static final String ID = "49cdfd18-328d-4fba-8714-8f687d7fa9ef";
    public static final String PARAM_INVOICE = "invoice";
    public static final String REPORT_NAME = "Invoice Report";

    static final String TEMPLATE_CODE = "DEFAULT";
    static final String TEMPLATE_PATH = "com/company/crm/reports/templates/invoice.docx";
    static final String OUTPUT_NAME_PATTERN = "invoice-report.pdf";
    static final String PARAM_INVOICE_NAME = "Invoice";

    static final String BAND_ROOT = "Root";
    static final String BAND_INVOICE = "invoice";
    static final String BAND_ORDER = "order";
    static final String BAND_CLIENT = "client";
    static final String BAND_PAYMENT = "payment";
    static final String BAND_PRICE = "price";
    static final String BAND_QUANTITY = "quantity";
    static final String BAND_DISCOUNT = "discount";
    static final String BAND_VAT = "vat";
    static final String BAND_TOTAL = "total";
    static final String BAND_AMOUNT = "amount";

    static final String ALIAS_SEPARATOR = ".";
    static final String ALIAS_INVOICE = BAND_INVOICE;
    static final String ALIAS_CLIENT = BAND_CLIENT;
    static final String ALIAS_PAYMENT = BAND_PAYMENT;
    static final String ALIAS_INVOICE_ORDER = BAND_INVOICE + ALIAS_SEPARATOR + BAND_ORDER;

    static final String KEY_NUMBER = "number";
    static final String KEY_DATE = "date";
    static final String KEY_DUE_DATE = "due_date";
    static final String KEY_PURCHASE_ORDER = "purchase_order";
    static final String KEY_SUBTOTAL = "sutotal";
    static final String KEY_NAME = "name";
    static final String KEY_ADDRESS = "address";
    static final String KEY_ORDER_ITEM = "order_item";
    static final String KEY_PAYMENTS_SUM = "payments_sum";
    static final String KEY_PRICE = BAND_PRICE;
    static final String KEY_QUANTITY = BAND_QUANTITY;
    static final String KEY_DISCOUNT = BAND_DISCOUNT;
    static final String KEY_VAT = BAND_VAT;
    static final String KEY_TOTAL = BAND_TOTAL;
    static final String KEY_AMOUNT = BAND_AMOUNT;

    static final String PROP_CLIENT = BAND_CLIENT;
    static final String PROP_ADDRESS = KEY_ADDRESS;
    static final String PROP_ORDER = BAND_ORDER;
    static final String PROP_PURCHASE_ORDER = "purchaseOrder";
    static final String PROP_DATE = KEY_DATE;
    static final String PROP_ORDER_ITEMS = "orderItems";
    static final String PROP_CATEGORY_ITEM = "categoryItem";
    static final String PROP_PAYMENTS = "payments";

    private final DataManager dataManager;
    private final FetchPlans fetchPlans;

    public InvoiceReport(DataManager dataManager, FetchPlans fetchPlans) {
        this.dataManager = dataManager;
        this.fetchPlans = fetchPlans;
    }

    @DataSetDelegate(name = InvoiceReport.BAND_INVOICE)
    public ReportDataLoader invoiceDataLoader() {
        return (reportQuery, parentBand, params) -> {
            ReportData reportData = getReportData(params);
            Map<String, Object> map = new HashMap<>();
            Invoice invoice = reportData.invoice();
            Order order = reportData.order();
            Client client = reportData.client();

            if (invoice != null) {
                putWithInvoiceAlias(map, KEY_NUMBER, invoice.getNumber());
                putWithInvoiceAlias(map, KEY_DATE, invoice.getDate());
                putWithInvoiceAlias(map, KEY_DUE_DATE, invoice.getDueDate());
                putWithInvoiceAlias(map, KEY_PURCHASE_ORDER, order != null ? order.getPurchaseOrder() : null);
                putWithInvoiceAlias(map, KEY_SUBTOTAL, defaultIfNull(invoice.getSubtotal()));
                putWithInvoiceAlias(map, KEY_VAT, defaultIfNull(invoice.getVat()));
                putWithInvoiceAlias(map, KEY_TOTAL, defaultIfNull(invoice.getTotal()));
                putWithInvoiceAlias(map, KEY_PAYMENTS_SUM, defaultIfNull(invoice.getPaymentsSum()));
            }

            if (client != null) {
                putAliasOnly(map, ALIAS_CLIENT, KEY_NAME, client.getName());
                putAliasOnly(map, ALIAS_CLIENT, KEY_ADDRESS, clientAddress(client));
            }
            return List.of(map);
        };
    }

    @DataSetDelegate(name = InvoiceReport.BAND_ORDER)
    public ReportDataLoader orderDataLoader() {
        return (reportQuery, parentBand, params) -> {
            ReportData reportData = getReportData(params);
            Map<String, Object> map = new HashMap<>();
            Order order = reportData.order();
            OrderItem orderItem = reportData.orderItem();
            putWithAlias(map, ALIAS_INVOICE_ORDER, KEY_DATE, order != null ? order.getDate() : null);
            putWithAlias(map, ALIAS_INVOICE_ORDER, KEY_ORDER_ITEM, orderItemDescription(orderItem));
            map.put(KEY_PRICE, orderItemValue(params, OrderItemValue.PRICE));
            map.put(KEY_QUANTITY, orderItemValue(params, OrderItemValue.QUANTITY));
            map.put(KEY_DISCOUNT, orderItemValue(params, OrderItemValue.DISCOUNT));
            map.put(KEY_VAT, orderItemValue(params, OrderItemValue.VAT));
            map.put(KEY_TOTAL, orderItemValue(params, OrderItemValue.TOTAL));
            return List.of(map);
        };
    }

    @DataSetDelegate(name = InvoiceReport.BAND_CLIENT)
    public ReportDataLoader clientDataLoader() {
        return (reportQuery, parentBand, params) -> {
            ReportData reportData = getReportData(params);
            Map<String, Object> map = new HashMap<>();
            Client client = reportData.client();
            putWithAlias(map, ALIAS_CLIENT, KEY_NAME, client != null ? client.getName() : null);
            putWithAlias(map, ALIAS_CLIENT, KEY_ADDRESS, clientAddress(client));
            return List.of(map);
        };
    }

    @DataSetDelegate(name = InvoiceReport.BAND_PAYMENT)
    public ReportDataLoader paymentDataLoader() {
        return (reportQuery, parentBand, params) -> {
            ReportData reportData = getReportData(params);
            Map<String, Object> map = new HashMap<>();
            Payment payment = reportData.payment();
            putWithAlias(map, ALIAS_PAYMENT, KEY_DATE, payment != null ? payment.getDate() : null);
            map.put(KEY_AMOUNT, payment != null ? payment.getAmount() : null);
            putAliasOnly(map, ALIAS_INVOICE, KEY_DUE_DATE, reportData.invoice() != null ? reportData.invoice().getDueDate() : null);
            return List.of(map);
        };
    }

    @DataSetDelegate(name = InvoiceReport.BAND_PRICE)
    public ReportDataLoader priceDataLoader() {
        return (reportQuery, parentBand, params) -> List.of(valueMap(KEY_PRICE, orderItemValue(params, OrderItemValue.PRICE)));
    }

    @DataSetDelegate(name = InvoiceReport.BAND_QUANTITY)
    public ReportDataLoader quantityDataLoader() {
        return (reportQuery, parentBand, params) -> List.of(valueMap(KEY_QUANTITY, orderItemValue(params, OrderItemValue.QUANTITY)));
    }

    @DataSetDelegate(name = InvoiceReport.BAND_DISCOUNT)
    public ReportDataLoader discountDataLoader() {
        return (reportQuery, parentBand, params) -> List.of(valueMap(KEY_DISCOUNT, orderItemValue(params, OrderItemValue.DISCOUNT)));
    }

    @DataSetDelegate(name = InvoiceReport.BAND_VAT)
    public ReportDataLoader vatDataLoader() {
        return (reportQuery, parentBand, params) -> List.of(valueMap(KEY_VAT, orderItemValue(params, OrderItemValue.VAT)));
    }

    @DataSetDelegate(name = InvoiceReport.BAND_TOTAL)
    public ReportDataLoader totalDataLoader() {
        return (reportQuery, parentBand, params) -> List.of(valueMap(KEY_TOTAL, orderItemValue(params, OrderItemValue.TOTAL)));
    }

    @DataSetDelegate(name = InvoiceReport.BAND_AMOUNT)
    public ReportDataLoader amountDataLoader() {
        return (reportQuery, parentBand, params) -> {
            ReportData reportData = getReportData(params);
            Payment payment = reportData.payment();
            BigDecimal amount = payment != null ? payment.getAmount() : null;
            return List.of(valueMap(KEY_AMOUNT, amount));
        };
    }

    private ReportData getReportData(Map<String, Object> params) {
        Object cached = params.get(ReportData.CACHE_KEY);
        if (cached instanceof ReportData reportData) {
            return reportData;
        }

        Invoice invoice = reloadInvoice(params);
        ReportData reportData = new ReportData(invoice);
        params.put(ReportData.CACHE_KEY, reportData);

        return reportData;
    }

    private Invoice reloadInvoice(Map<String, Object> params) {
        Object param = params.get(PARAM_INVOICE);
        if (!(param instanceof Invoice invoice)) {
            return null;
        }
        FetchPlan fetchPlan = fetchPlans.builder(Invoice.class)
                .addFetchPlan(FetchPlan.BASE)
                .add(PROP_CLIENT,
                        client -> client.addFetchPlan(FetchPlan.BASE).add(PROP_ADDRESS))
                .add(PROP_ORDER,
                        order -> order.addFetchPlan(FetchPlan.BASE)
                                .add(PROP_PURCHASE_ORDER)
                                .add(PROP_DATE)
                                .add(PROP_ORDER_ITEMS,
                                        item -> item.addFetchPlan(FetchPlan.BASE).add(PROP_CATEGORY_ITEM, FetchPlan.BASE))
                )
                .add(PROP_PAYMENTS,
                        payment -> payment.addFetchPlan(FetchPlan.BASE))
                .build();

        return dataManager.load(Invoice.class)
                .id(invoice.getId())
                .fetchPlan(fetchPlan)
                .one();
    }

    private static Map<String, Object> valueMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static void putWithInvoiceAlias(Map<String, Object> map, String key, Object value) {
        putWithAlias(map, ALIAS_INVOICE, key, value);
    }

    private static void putWithAlias(Map<String, Object> map, String alias, String key, Object value) {
        map.put(key, value);
        map.put(alias + ALIAS_SEPARATOR + key, value);
    }

    private static void putAliasOnly(Map<String, Object> map, String alias, String key, Object value) {
        map.put(alias + ALIAS_SEPARATOR + key, value);
    }

    private static String clientAddress(Client client) {
        if (client == null || client.getAddress() == null) {
            return null;
        }
        return client.getAddress().getInstanceName();
    }

    private static String orderItemDescription(OrderItem orderItem) {
        if (orderItem == null || orderItem.getCategoryItem() == null) {
            return null;
        }
        return orderItem.getCategoryItem().getName();
    }

    private static BigDecimal defaultIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Object orderItemValue(Map<String, Object> params, OrderItemValue value) {
        ReportData reportData = getReportData(params);
        OrderItem orderItem = reportData.orderItem();
        if (orderItem == null) {
            return null;
        }
        return value.extract(orderItem);
    }

    private enum OrderItemValue implements OrderItemValueExtractor {
        PRICE {
            @Override
            public Object extract(OrderItem item) {
                return item.getUnitPrice();
            }
        },
        QUANTITY {
            @Override
            public Object extract(OrderItem item) {
                return item.getQuantity();
            }
        },
        DISCOUNT {
            @Override
            public Object extract(OrderItem item) {
                return item.getDiscount();
            }
        },
        VAT {
            @Override
            public Object extract(OrderItem item) {
                return item.getVat();
            }
        },
        TOTAL {
            @Override
            public Object extract(OrderItem item) {
                return item.getTotal();
            }
        }
    }

    @FunctionalInterface
    private interface OrderItemValueExtractor {
        Object extract(OrderItem item);
    }

    private record ReportData(Invoice invoice) {

        public static final String CACHE_KEY = "invoice_cache";

        Client client() {
            return invoice != null ? invoice.getClient() : null;
        }

        Order order() {
            return invoice != null ? invoice.getOrder() : null;
        }

        OrderItem orderItem() {
            Order order = order();
            List<OrderItem> items = order != null ? order.getOrderItems() : null;
            if (items == null || items.isEmpty()) {
                return null;
            }
            return items.getFirst();
        }

        Payment payment() {
            List<Payment> payments = invoice != null ? invoice.getPayments() : null;
            if (payments == null || payments.isEmpty()) {
                return null;
            }
            return payments.getFirst();
        }
    }
}
