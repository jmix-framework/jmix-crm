package com.company.crm.app.service.analytics;

import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.payment.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Domain policy: aging and temporal interpretation for receivables.
 */
@Component("crm_ReceivablesAgingPolicy")
public class ReceivablesAgingPolicy {

    public Long resolveDaysToCash(Invoice invoice, Payment payment) {
        if (invoice.getDate() == null || payment.getDate() == null) {
            return null;
        }
        return Math.max(0, ChronoUnit.DAYS.between(invoice.getDate(), payment.getDate()));
    }

    public boolean isReceivableAtRisk(Invoice invoice, LocalDate asOfDate, BigDecimal invoiceOpenTotal) {
        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            return false;
        }
        if (InvoiceStatus.OVERDUE.equals(invoice.getStatus())) {
            return true;
        }
        return invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(asOfDate)
                && invoiceOpenTotal.compareTo(BigDecimal.ZERO) > 0;
    }

    public long resolveDaysOverdue(Invoice invoice, LocalDate asOfDate) {
        if (invoice.getDueDate() == null || asOfDate == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.DAYS.between(invoice.getDueDate(), asOfDate));
    }
}
