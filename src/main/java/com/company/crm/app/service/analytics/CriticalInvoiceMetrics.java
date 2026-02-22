package com.company.crm.app.service.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing a critical invoice contributing to category risk.
 */
public record CriticalInvoiceMetrics(
        String invoiceNumber,
        String clientName,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String invoiceStatus,
        String categoryCode,
        String categoryName,
        BigDecimal categoryOpenAmount,
        long daysOverdue
) {
}
