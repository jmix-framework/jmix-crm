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
        Invoice invoice = new Invoice();
        Payment payment = new Payment();

        assertThat(policy.resolveDaysToCash(invoice, payment)).isNull();
    }

    @Test
    void resolveDaysToCash_clampsNegativeToZero() {
        Invoice invoice = new Invoice();
        invoice.setDate(LocalDate.of(2026, 2, 10));
        Payment payment = new Payment();
        payment.setDate(LocalDate.of(2026, 2, 5));

        assertThat(policy.resolveDaysToCash(invoice, payment)).isEqualTo(0L);
    }

    @Test
    void isReceivableAtRisk_byStatusOverdue() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.OVERDUE);

        assertThat(policy.isReceivableAtRisk(invoice, LocalDate.of(2026, 2, 21), new BigDecimal("10.00"))).isTrue();
    }

    @Test
    void isReceivableAtRisk_byDueDateAndOpenAmount() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setDueDate(LocalDate.of(2026, 2, 1));

        assertThat(policy.isReceivableAtRisk(invoice, LocalDate.of(2026, 2, 21), new BigDecimal("1.00"))).isTrue();
    }

    @Test
    void isReceivableAtRisk_paidInvoiceIsNeverOverdue() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setDueDate(LocalDate.of(2026, 2, 1));

        assertThat(policy.isReceivableAtRisk(invoice, LocalDate.of(2026, 2, 21), new BigDecimal("100.00"))).isFalse();
    }

    @Test
    void resolveDaysOverdue_clampsNegativeToZero() {
        Invoice invoice = new Invoice();
        invoice.setDueDate(LocalDate.of(2026, 2, 25));

        assertThat(policy.resolveDaysOverdue(invoice, LocalDate.of(2026, 2, 21))).isEqualTo(0L);
    }

    @Test
    void resolveDaysOverdue_returnsDifference() {
        Invoice invoice = new Invoice();
        invoice.setDueDate(LocalDate.of(2026, 2, 1));

        assertThat(policy.resolveDaysOverdue(invoice, LocalDate.of(2026, 2, 21))).isEqualTo(20L);
    }
}
