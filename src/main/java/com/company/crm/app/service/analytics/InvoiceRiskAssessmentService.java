package com.company.crm.app.service.analytics;

import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates category share, payment allocation and temporal rules per invoice.
 */
@Component("crm_InvoiceRiskAssessmentService")
public class InvoiceRiskAssessmentService {

    private final CategoryAllocationPolicy categoryAllocationPolicy;
    private final PaymentSettlementPolicy paymentSettlementPolicy;
    private final ReceivablesAgingPolicy receivablesAgingPolicy;

    public InvoiceRiskAssessmentService(CategoryAllocationPolicy categoryAllocationPolicy,
                                        PaymentSettlementPolicy paymentSettlementPolicy,
                                        ReceivablesAgingPolicy receivablesAgingPolicy) {
        this.categoryAllocationPolicy = categoryAllocationPolicy;
        this.paymentSettlementPolicy = paymentSettlementPolicy;
        this.receivablesAgingPolicy = receivablesAgingPolicy;
    }

    public InvoiceRiskAssessmentResult assessInvoiceRisk(Invoice invoice, List<OrderItem> orderItems, LocalDate asOfDate) {
        BigDecimal invoiceTotal = safeMoney(invoice.getTotal());
        if (invoiceTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new InvoiceRiskAssessmentResult(List.of(), BigDecimal.ZERO, List.of());
        }

        Map<String, CategoryAllocationPolicy.CategoryAllocationShare> shares = categoryAllocationPolicy.defineAllocationShares(orderItems);
        LinkedHashMap<String, BigDecimal> allocatedByCategory = categoryAllocationPolicy.allocateExposure(invoiceTotal, shares);
        Map<String, BigDecimal> paidByCategory = zeroMoneyMap(allocatedByCategory);
        Map<String, BigDecimal> remainingByCategory = new LinkedHashMap<>(allocatedByCategory);
        Map<String, BigDecimal> dtcNumerator = zeroMoneyMap(allocatedByCategory);
        Map<String, BigDecimal> dtcDenominator = zeroMoneyMap(allocatedByCategory);
        Map<String, Long> paymentsCount = zeroCountMap(allocatedByCategory);

        BigDecimal overpayment = BigDecimal.ZERO;
        for (Payment payment : sortedPayments(invoice)) {
            BigDecimal paymentAmount = safeMoney(payment.getAmount());
            if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            PaymentSettlementPolicy.PaymentSettlement settlement =
                    paymentSettlementPolicy.settle(paymentAmount, remainingByCategory);
            overpayment = overpayment.add(settlement.overpayment());

            Long days = receivablesAgingPolicy.resolveDaysToCash(invoice, payment);
            for (Map.Entry<String, BigDecimal> allocation : settlement.distributed().entrySet()) {
                String categoryCode = allocation.getKey();
                BigDecimal allocatedPayment = allocation.getValue();

                paidByCategory.put(categoryCode, paidByCategory.get(categoryCode).add(allocatedPayment));
                remainingByCategory.put(categoryCode, remainingByCategory.get(categoryCode).subtract(allocatedPayment));
                paymentsCount.put(categoryCode, paymentsCount.get(categoryCode) + 1L);

                if (days != null && allocatedPayment.compareTo(BigDecimal.ZERO) > 0) {
                    dtcNumerator.put(categoryCode, dtcNumerator.get(categoryCode).add(allocatedPayment.multiply(BigDecimal.valueOf(days))));
                    dtcDenominator.put(categoryCode, dtcDenominator.get(categoryCode).add(allocatedPayment));
                }
            }
        }

        BigDecimal invoiceOpenTotal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : allocatedByCategory.entrySet()) {
            BigDecimal open = entry.getValue().subtract(paidByCategory.get(entry.getKey())).max(BigDecimal.ZERO);
            invoiceOpenTotal = invoiceOpenTotal.add(open);
        }
        boolean overdue = receivablesAgingPolicy.isReceivableAtRisk(invoice, asOfDate, invoiceOpenTotal);

        List<CategoryRiskPosition> categories = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : allocatedByCategory.entrySet()) {
            String categoryCode = entry.getKey();
            CategoryAllocationPolicy.CategoryAllocationShare share = shares.get(categoryCode);
            BigDecimal invoiced = entry.getValue();
            BigDecimal paid = paidByCategory.get(categoryCode);
            BigDecimal open = invoiced.subtract(paid).max(BigDecimal.ZERO);
            BigDecimal overdueOpen = overdue ? open : BigDecimal.ZERO;

            categories.add(new CategoryRiskPosition(
                    categoryCode,
                    share.name(),
                    invoiced,
                    paid,
                    open,
                    overdueOpen,
                    dtcNumerator.get(categoryCode),
                    dtcDenominator.get(categoryCode),
                    paymentsCount.get(categoryCode)
            ));
        }

        List<CriticalInvoiceMetrics> criticalInvoices = List.of();
        if (overdue) {
            long daysOverdue = receivablesAgingPolicy.resolveDaysOverdue(invoice, asOfDate);
            criticalInvoices = categories.stream()
                    .filter(category -> category.openAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(category -> new CriticalInvoiceMetrics(
                            resolveInvoiceNumber(invoice),
                            invoice.getClient() != null ? invoice.getClient().getName() : null,
                            invoice.getDate(),
                            invoice.getDueDate(),
                            invoice.getStatus() != null ? invoice.getStatus().name() : null,
                            category.categoryCode(),
                            category.categoryName(),
                            category.openAmount(),
                            daysOverdue
                    ))
                    .toList();
        }

        return new InvoiceRiskAssessmentResult(categories, overpayment, criticalInvoices);
    }

    private List<Payment> sortedPayments(Invoice invoice) {
        if (invoice.getPayments() == null || invoice.getPayments().isEmpty()) {
            return List.of();
        }
        return invoice.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Payment::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Map<String, BigDecimal> zeroMoneyMap(Map<String, BigDecimal> source) {
        LinkedHashMap<String, BigDecimal> zeros = new LinkedHashMap<>();
        source.keySet().forEach(key -> zeros.put(key, BigDecimal.ZERO));
        return zeros;
    }

    private Map<String, Long> zeroCountMap(Map<String, BigDecimal> source) {
        LinkedHashMap<String, Long> zeros = new LinkedHashMap<>();
        source.keySet().forEach(key -> zeros.put(key, 0L));
        return zeros;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String resolveInvoiceNumber(Invoice invoice) {
        try {
            return invoice.getNumber();
        } catch (Exception ignored) {
            return null;
        }
    }

    public record CategoryRiskPosition(
            String categoryCode,
            String categoryName,
            BigDecimal invoicedAmount,
            BigDecimal paidAmount,
            BigDecimal openAmount,
            BigDecimal overdueOpenAmount,
            BigDecimal dtcNumerator,
            BigDecimal dtcDenominator,
            long paymentsCount
    ) {
    }

    public record InvoiceRiskAssessmentResult(
            List<CategoryRiskPosition> categories,
            BigDecimal overpaymentAmount,
            List<CriticalInvoiceMetrics> criticalInvoices
    ) {
    }
}
