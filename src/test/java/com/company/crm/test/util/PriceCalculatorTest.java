package com.company.crm.test.util;

import com.company.crm.app.util.price.PriceCalculator;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderDiscountType;
import com.company.crm.model.order.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JmixIncorrectCreateEntity") // Pure calculations do not require a persistence context.
class PriceCalculatorTest {

    @Test
    void sumsAllLineAmountsWithoutMultiplyingQuantityTwice() {
        OrderItem first = item("100", "2", "0", "0");
        OrderItem second = item("50", "3", "0", "0");
        Order order = order(first, second);

        assertThat(first.getNetPrice()).isEqualByComparingTo("200");
        assertThat(second.getNetPrice()).isEqualByComparingTo("150");
        assertThat(order.getSubTotal()).isEqualByComparingTo("350");
        assertThat(order.getVat()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("350");
    }

    @Test
    void sumsLineVatAndDiscountsOnce() {
        OrderItem first = item("100", "2", "40", "10");
        OrderItem second = item("50", "3", "30", "0");
        Order order = order(first, second);

        assertThat(first.getTotal()).isEqualByComparingTo("230");
        assertThat(second.getTotal()).isEqualByComparingTo("180");
        assertThat(order.getSubTotal()).isEqualByComparingTo("340");
        assertThat(order.getVat()).isEqualByComparingTo("70");
        assertThat(order.getTotal()).isEqualByComparingTo("410");
    }

    @Test
    void appliesOrderDiscountAfterSummingLineAmounts() {
        Order order = order(item("100", "2", "40", "10"), item("50", "3", "30", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        assertThat(order.getTotal()).isEqualByComparingTo("369");
        assertThat(order.getSubTotal()).isEqualByComparingTo("306");
        assertThat(order.getVat()).isEqualByComparingTo("63");

        order.setDiscountPercent(BigDecimal.ZERO);
        order.setDiscountValue(new BigDecimal("25"));
        assertThat(order.getTotal()).isEqualByComparingTo("385");
        assertThat(order.getSubTotal()).isEqualByComparingTo("319.27");
        assertThat(order.getVat()).isEqualByComparingTo("65.73");
    }

    @Test
    void emptyOrderHasZeroSummary() {
        Order order = new Order();
        assertThat(order.getSubTotal()).isEqualByComparingTo("0");
        assertThat(order.getVat()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("0");
    }

    @Test
    void amountDiscountPreservesCentsDespiteTheRoundedPercentage() {
        Order order = order(item("1000", "1", "0", "0"));
        order.setDiscountValue(new BigDecimal("0.05"));
        order.setDiscountPercent(new BigDecimal("0.01"));
        order.setDiscountType(OrderDiscountType.AMOUNT);

        assertSummary(order, "999.95", "0", "999.95");
    }

    @Test
    void percentageDiscountIgnoresAnAmountFromAnotherMode() {
        Order order = order(item("100", "1", "20", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountValue(new BigDecimal("99"));
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "90", "18", "108");

        order.setDiscountPercent(BigDecimal.ZERO);

        assertSummary(order, "100", "20", "120");
    }

    @Test
    void legacyDiscountUsesThePercentageBeforeTheAmount() {
        Order order = order(item("100", "1", "20", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountValue(new BigDecimal("25"));

        assertThat(order.getDiscountType()).isNull();
        assertThat(PriceCalculator.resolveDiscountType(order)).isEqualTo(OrderDiscountType.PERCENT);
        assertSummary(order, "90", "18", "108");

        order.setDiscountPercent(BigDecimal.ZERO);

        assertThat(order.getDiscountType()).isNull();
        assertThat(PriceCalculator.resolveDiscountType(order)).isEqualTo(OrderDiscountType.AMOUNT);
        assertSummary(order, "79.17", "15.83", "95");
    }

    @Test
    void amountDiscountRemainsFixedWhenItemsChange() {
        OrderItem first = item("100", "1", "0", "0");
        Order order = order(first);
        order.setDiscountValue(new BigDecimal("25"));
        order.setDiscountPercent(new BigDecimal("25"));
        order.setDiscountType(OrderDiscountType.AMOUNT);
        assertSummary(order, "75", "0", "75");

        order.setOrderItems(List.of(first, item("100", "1", "0", "0")));

        assertSummary(order, "175", "0", "175");
    }

    @Test
    void roundedSubtotalAndVatAddUpToTheRoundedTotal() {
        Order order = order(item("0.04", "1", "0.01", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "0.03", "0.01", "0.04");
    }

    @Test
    void fullAmountAndPercentageDiscountsHaveZeroSummary() {
        Order order = order(item("100", "1", "20", "0"));
        order.setDiscountValue(new BigDecimal("120"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountType(OrderDiscountType.AMOUNT);
        assertSummary(order, "0", "0", "0");

        order.setDiscountPercent(new BigDecimal("100"));
        order.setDiscountValue(BigDecimal.ZERO);
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "0", "0", "0");
    }

    @Test
    void zeroPricedItemsHaveZeroSummaryWithAPercentageDiscount() {
        Order order = order(item("0", "1", "0", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "0", "0", "0");
    }

    @Test
    void invoiceInheritsOrderNetAndVatWithoutChargingVatTwice() {
        Order order = order(item("100", "1", "20", "0"));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "100", "20", "120");
    }

    @Test
    void invoiceInheritsLineAndOrderDiscounts() {
        Order order = order(item("100", "2", "40", "10"), item("50", "3", "30", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "306", "63", "369");
    }

    @Test
    void invoicePreservesVatFreeOrder() {
        Order order = order(item("100", "1", "0", "0"));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "100", "0", "100");
    }

    @Test
    void invoicePreservesMixedVatAmountsFromTheOrder() {
        Order order = order(item("100", "1", "20", "0"), item("100", "1", "0", "0"));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "200", "20", "220");
    }

    @Test
    void invoiceSubtractsPreviousNetAndVatSeparately() {
        Order order = order(item("100", "1", "20", "0"));
        order.setInvoices(List.of(invoice("40", "8")));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "60", "12", "72");
    }

    @Test
    void invoiceExcludesItsOwnPersistedIdFromTheRemainingAmounts() {
        Order order = order(item("100", "1", "20", "0"));
        Invoice savedInvoice = invoice("60", "12");
        order.setInvoices(List.of(invoice("40", "8"), savedInvoice));
        Invoice editedInvoice = invoice("60", "12");
        editedInvoice.setId(savedInvoice.getId());

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, editedInvoice);

        assertInvoiceSummary(editedInvoice, "60", "12", "72");
    }

    @Test
    void invoiceClearsPreviousAmountsWhenTheOrderIsFullyInvoiced() {
        Order order = order(item("100", "1", "20", "0"));
        order.setInvoices(List.of(invoice("100", "20")));
        Invoice invoice = invoice("50", "10");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "0", "0", "0");
    }

    @Test
    void invoiceAutofillDoesNotProduceNegativeAmountsForAnOverInvoicedOrder() {
        Order order = order(item("100", "1", "20", "0"));
        order.setInvoices(List.of(invoice("110", "25")));
        Invoice invoice = invoice("5", "1");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "0", "0", "0");
        assertThat(order.getInvoiced()).isEqualByComparingTo("135");
    }

    @Test
    void invoiceClearsAmountsWhenPreviousGrossTotalExhaustsTheOrder() {
        Order order = order(item("100", "1", "20", "0"));
        order.setInvoices(List.of(invoice("120", "0")));
        Invoice invoice = invoice("5", "1");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "0", "0", "0");
    }

    @Test
    void invoiceCapsVatRemainderByGrossBalanceWhenNetIsOverInvoiced() {
        Order order = order(item("100", "1", "20", "0"));
        order.setInvoices(List.of(invoice("110", "0")));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "0", "10", "10");
    }

    @Test
    void invoiceCapsNetRemainderByGrossBalanceWhenVatIsOverInvoiced() {
        Order order = order(item("100", "1", "20", "0"));
        order.setInvoices(List.of(invoice("80", "30")));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "10", "0", "10");
    }

    @Test
    void invoiceClearsPreviousAmountsForAnEmptyOrder() {
        Order order = order();
        Invoice invoice = invoice("5", "1");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "0", "0", "0");
    }

    @Test
    void invoicePreservesTheOrdersRoundedVatRemainder() {
        Order order = order(item("0.04", "1", "0.01", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setInvoices(List.of(invoice("0.01", "0")));
        Invoice invoice = invoice("0", "0");

        PriceCalculator.calculateInvoiceFieldsFromOrder(order, invoice);

        assertInvoiceSummary(invoice, "0.02", "0.01", "0.03");
    }

    @Test
    void invoicedAmountSumsGrossInvoiceTotals() {
        Order order = order(item("100", "1", "20", "0"));
        order.setInvoices(List.of(invoice("40", "8"), invoice("60", "12")));

        assertThat(order.getInvoiced()).isEqualByComparingTo("120");
    }

    private void assertInvoiceSummary(Invoice invoice, String subtotal, String vat, String total) {
        assertThat(invoice.getSubtotal()).isEqualByComparingTo(subtotal);
        assertThat(invoice.getVat()).isEqualByComparingTo(vat);
        assertThat(invoice.getTotal()).isEqualByComparingTo(total);
        assertThat(invoice.getSubtotal().add(invoice.getVat())).isEqualByComparingTo(invoice.getTotal());
    }

    private Invoice invoice(String subtotal, String vat) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setSubtotal(new BigDecimal(subtotal));
        invoice.setVat(new BigDecimal(vat));
        invoice.setTotal(invoice.getSubtotal().add(invoice.getVat()));
        return invoice;
    }

    private void assertSummary(Order order, String subtotal, String vat, String total) {
        assertThat(order.getSubTotal()).isEqualByComparingTo(subtotal);
        assertThat(order.getVat()).isEqualByComparingTo(vat);
        assertThat(order.getTotal()).isEqualByComparingTo(total);
        assertThat(order.getSubTotal().add(order.getVat())).isEqualByComparingTo(order.getTotal());
    }

    private OrderItem item(String unitPrice, String quantity, String vat, String discount) {
        CategoryItem product = new CategoryItem();
        product.setPrice(new BigDecimal(unitPrice));
        OrderItem item = new OrderItem();
        item.setCategoryItem(product);
        item.setQuantity(new BigDecimal(quantity));
        item.setVat(new BigDecimal(vat));
        item.setDiscount(new BigDecimal(discount));
        PriceCalculator.recalculatePricing(item);
        return item;
    }

    private Order order(OrderItem... items) {
        Order order = new Order();
        order.setOrderItems(List.of(items));
        return order;
    }
}
