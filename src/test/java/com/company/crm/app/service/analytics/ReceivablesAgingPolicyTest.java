package com.company.crm.app.service.analytics;

import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReceivablesAgingPolicyTest {

    private final ReceivablesAgingPolicy policy = new ReceivablesAgingPolicy();

    @Test
    void resolveDaysToCash_returnsNullIfDatesMissing() {
        // given
        Invoice invoice = new Invoice();
        Payment payment = new Payment();

        // when / then
        assertThat(policy.resolveDaysToCash(invoice, payment)).isNull();
    }

    @Test
    void resolveDaysToCash_clampsNegativeToZero() {
        // given
        LocalDate invoiceDate = LocalDate.of(2026, 2, 10);
        LocalDate paymentDate = LocalDate.of(2026, 2, 5);
        Invoice invoice = new Invoice();
        invoice.setDate(invoiceDate);
        Payment payment = new Payment();
        payment.setDate(paymentDate);

        // when / then
        assertThat(policy.resolveDaysToCash(invoice, payment)).isEqualTo(0L);
    }

    @Test
    void isReceivableAtRisk_byStatusOverdue() {
        // given
        LocalDate asOfDate = LocalDate.of(2026, 2, 21);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.OVERDUE);

        // when / then
        assertThat(policy.isReceivableAtRisk(invoice, asOfDate, new BigDecimal("10.00"))).isTrue();
    }

    @Test
    void isReceivableAtRisk_byDueDateAndOpenAmount() {
        // given
        LocalDate dueDate = LocalDate.of(2026, 2, 1);
        LocalDate asOfDate = LocalDate.of(2026, 2, 21);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setDueDate(dueDate);

        // when / then
        assertThat(policy.isReceivableAtRisk(invoice, asOfDate, new BigDecimal("1.00"))).isTrue();
        assertThat(policy.isReceivableAtRisk(invoice, asOfDate, BigDecimal.ZERO)).isFalse();
    }

    @Test
    void isReceivableAtRisk_paidInvoiceIsNeverOverdue() {
        // given
        LocalDate dueDate = LocalDate.of(2026, 2, 1);
        LocalDate asOfDate = LocalDate.of(2026, 2, 21);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setDueDate(dueDate);

        // when / then
        assertThat(policy.isReceivableAtRisk(invoice, asOfDate, new BigDecimal("100.00"))).isFalse();
    }

    @Test
    void resolveDaysOverdue_clampsNegativeToZero() {
        // given
        LocalDate dueDate = LocalDate.of(2026, 2, 25);
        LocalDate asOfDate = LocalDate.of(2026, 2, 21);
        Invoice invoice = new Invoice();
        invoice.setDueDate(dueDate);

        // when / then
        assertThat(policy.resolveDaysOverdue(invoice, asOfDate)).isEqualTo(0L);
    }

    @Test
    void resolveDaysOverdue_returnsDifference() {
        // given
        LocalDate dueDate = LocalDate.of(2026, 2, 1);
        LocalDate asOfDate = LocalDate.of(2026, 2, 21);
        Invoice invoice = new Invoice();
        invoice.setDueDate(dueDate);

        // when / then
        assertThat(policy.resolveDaysOverdue(invoice, asOfDate)).isEqualTo(20L);
    }
}
