package com.company.crm.app.service.client;

import com.company.crm.app.service.finance.InvoiceService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.RiskLevel;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.report.config.ClientReportThresholds;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanBuilder;
import io.jmix.core.FetchPlans;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service providing business logic for Client 360 degree analysis and reporting.
 * Contains customer classification, risk assessment, and financial analysis methods.
 */
@Service
public class Client360ReportService {

    private static final Logger log = LoggerFactory.getLogger(Client360ReportService.class);

    // Customer value thresholds
    private static final BigDecimal HIGH_VALUE_THRESHOLD = BigDecimal.valueOf(50000);
    private static final double VIP_PAYMENT_RATE_THRESHOLD = 95.0;

    // Activity and relationship thresholds
    private static final int INACTIVE_THRESHOLD_DAYS = 90;
    private static final int NEW_CUSTOMER_THRESHOLD_DAYS = 30;
    private static final int FREQUENT_CUSTOMER_MIN_ORDERS = 12;

    // Risk assessment thresholds
    private static final int HIGH_RISK_OVERDUE_COUNT = 3;
    private static final BigDecimal HIGH_RISK_OVERDUE_AMOUNT = BigDecimal.valueOf(5000);
    private static final double HIGH_RISK_PAYMENT_DURATION = 45.0;
    private static final int MEDIUM_RISK_OVERDUE_COUNT = 1;
    private static final BigDecimal MEDIUM_RISK_OVERDUE_AMOUNT = BigDecimal.valueOf(1000);
    private static final double MEDIUM_RISK_PAYMENT_DURATION = 30.0;

    // Payment and financial thresholds
    private static final double GOOD_PAYMENT_RATE_THRESHOLD = 90.0;
    private static final double GOOD_PAYMENT_DURATION_THRESHOLD = 30.0;

    private final DataManager dataManager;
    private final ClientService clientService;
    private final InvoiceService invoiceService;
    private final FetchPlans fetchPlans;

    public Client360ReportService(DataManager dataManager, ClientService clientService,
                                  InvoiceService invoiceService, FetchPlans fetchPlans) {
        this.dataManager = dataManager;
        this.clientService = clientService;
        this.invoiceService = invoiceService;
        this.fetchPlans = fetchPlans;
    }

    /**
     * Determines if a client is a high-value customer based on total invoiced amount.
     * @param client The client to evaluate
     * @param dateRange Optional date range for calculations (currently unused for this metric)
     * @return true if total invoiced amount exceeds HIGH_VALUE_THRESHOLD (€50,000)
     */
    public boolean isHighValueCustomer(Client client, @Nullable LocalDateRange dateRange) {
        try {
            BigDecimal totalInvoiced = clientService.getInvoicesTotalSum(client);
            return totalInvoiced.compareTo(HIGH_VALUE_THRESHOLD) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client qualifies as a VIP customer.
     * VIP criteria: high value, frequent orders, no payment issues, and excellent payment rate (>95%).
     * @param client The client to evaluate
     * @param dateRange Date range for calculations
     * @return true if client meets all VIP criteria
     */
    public boolean isVIPCustomer(Client client, @Nullable LocalDateRange dateRange) {
        try {
            boolean highValue = isHighValueCustomer(client, dateRange);
            boolean frequent = isFrequentCustomer(client, dateRange);
            boolean noPaymentIssues = !hasPaymentIssues(client, dateRange);

            BigDecimal totalInvoiced = clientService.getInvoicesTotalSum(client);
            BigDecimal totalPaid = clientService.getPaymentsTotalSum(client);
            double paymentRate = calculatePaymentRate(totalInvoiced, totalPaid);

            return highValue && frequent && noPaymentIssues && paymentRate > VIP_PAYMENT_RATE_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client is a new customer based on creation date.
     * @param client The client to evaluate
     * @return true if client was created within NEW_CUSTOMER_THRESHOLD_DAYS (30 days)
     */
    public boolean isNewCustomer(Client client) {
        try {
            if (client.getCreatedDate() == null) {
                return false;
            }

            LocalDate createdDate = client.getCreatedDate().toLocalDate();
            long daysSinceCreation = ChronoUnit.DAYS.between(createdDate, LocalDate.now());
            return daysSinceCreation <= NEW_CUSTOMER_THRESHOLD_DAYS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client is a frequent customer based on order frequency.
     * @param client The client to evaluate
     * @param dateRange Date range for calculations
     * @return true if client has >= FREQUENT_CUSTOMER_MIN_ORDERS (12) orders per year
     */
    public boolean isFrequentCustomer(Client client, @Nullable LocalDateRange dateRange) {
        try {
            if (dateRange == null) {
                return false;
            }

            long daysBetween = ChronoUnit.DAYS.between(dateRange.startDate(), dateRange.endDate());
            double yearsFactor = daysBetween / 365.0;

            Long orderCount = clientService.getCompletedOrdersAmount(dateRange, client);
            double ordersPerYear = orderCount / Math.max(yearsFactor, 0.1);
            return ordersPerYear >= FREQUENT_CUSTOMER_MIN_ORDERS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client is inactive based on last transaction date.
     * @param client The client to evaluate
     * @param dateRange Date range for calculations (optional)
     * @return true if client has no activity within INACTIVE_THRESHOLD_DAYS (90 days)
     */
    public boolean isInactiveCustomer(Client client, @Nullable LocalDateRange dateRange) {
        try {
            LocalDate lastActivity = clientService.getLastTransactionDate(client);
            if (lastActivity == null) {
                return true; // No activity at all
            }

            long daysSinceActivity = ChronoUnit.DAYS.between(lastActivity, LocalDate.now());
            return daysSinceActivity > INACTIVE_THRESHOLD_DAYS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client has payment issues based on overdue invoices and payment duration.
     * @param client The client to evaluate
     * @param dateRange Date range for calculations
     * @return true if client has overdue invoices or average payment duration > 30 days
     */
    public boolean hasPaymentIssues(Client client, @Nullable LocalDateRange dateRange) {
        try {
            if (dateRange == null) {
                return false;
            }

            long overdueCount = invoiceService.getInvoicesCountForClient(client, dateRange, InvoiceStatus.OVERDUE);
            double avgPaymentDuration = calculateAveragePaymentDuration(client, dateRange);

            return overdueCount > 0 || avgPaymentDuration > GOOD_PAYMENT_DURATION_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client has good payment history.
     * @param client The client to evaluate
     * @param dateRange Date range for calculations
     * @return true if payment rate > 90% and average payment duration <= 30 days
     */
    public boolean hasGoodPaymentHistory(Client client, @Nullable LocalDateRange dateRange) {
        try {
            BigDecimal totalInvoiced = clientService.getInvoicesTotalSum(client);
            BigDecimal totalPaid = clientService.getPaymentsTotalSum(client);
            double paymentRate = calculatePaymentRate(totalInvoiced, totalPaid);
            double avgPaymentDuration = calculateAveragePaymentDuration(client, dateRange);

            return paymentRate > GOOD_PAYMENT_RATE_THRESHOLD && avgPaymentDuration <= GOOD_PAYMENT_DURATION_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculates payment rate as percentage of total paid vs total invoiced.
     * @param totalInvoiced Total amount invoiced
     * @param totalPaid Total amount paid
     * @return Payment rate as percentage (0.0 - 100.0)
     */
    public double calculatePaymentRate(BigDecimal totalInvoiced, BigDecimal totalPaid) {
        if (totalInvoiced == null || totalInvoiced.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        if (totalPaid == null) {
            totalPaid = BigDecimal.ZERO;
        }
        return totalPaid.divide(totalInvoiced, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * Calculates average payment duration for paid invoices.
     * @param client The client to evaluate
     * @param dateRange Date range for calculations
     * @return Average payment duration in days
     */
    public double calculateAveragePaymentDuration(Client client, @Nullable LocalDateRange dateRange) {
        try {
            log.info("calculateAveragePaymentDuration called for client: {} with dateRange: {}", client.getId(), dateRange);

            if (dateRange == null) {
                log.info("DateRange is null, returning 0.0");
                return 0.0;
            }

            // Build FetchPlan to load invoices with their payments
            FetchPlan invoiceFetchPlan = fetchPlans.builder(com.company.crm.model.invoice.Invoice.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .build();

            // Load paid invoices for the client in the date range
            List<com.company.crm.model.invoice.Invoice> paidInvoices = dataManager.load(com.company.crm.model.invoice.Invoice.class)
                    .query("SELECT i FROM Invoice i WHERE i.client.id = :clientId " +
                           "AND i.date BETWEEN :fromDate AND :toDate " +
                           "AND i.status = :paidStatus")
                    .parameter("clientId", client.getId())
                    .parameter("fromDate", dateRange.startDate())
                    .parameter("toDate", dateRange.endDate())
                    .parameter("paidStatus", InvoiceStatus.PAID)
                    .fetchPlan(invoiceFetchPlan)
                    .list();

            log.info("Found {} paid invoices", paidInvoices.size());

            if (paidInvoices.isEmpty()) {
                log.info("No paid invoices found, returning 0.0");
                return 0.0;
            }

            long totalDays = 0;
            int count = 0;

            for (com.company.crm.model.invoice.Invoice invoice : paidInvoices) {
                LocalDate invoiceDate = invoice.getDate();

                // Build FetchPlan for payments
                FetchPlan paymentFetchPlan = fetchPlans.builder(com.company.crm.model.payment.Payment.class)
                        .addFetchPlan(FetchPlan.BASE)
                        .build();

                // Get payments for this invoice
                List<com.company.crm.model.payment.Payment> payments = dataManager.load(com.company.crm.model.payment.Payment.class)
                        .query("SELECT p FROM Payment p WHERE p.invoice.id = :invoiceId AND p.date IS NOT NULL")
                        .parameter("invoiceId", invoice.getId())
                        .fetchPlan(paymentFetchPlan)
                        .list();

                log.info("Invoice {} has {} payments", invoice.getId(), payments.size());

                for (com.company.crm.model.payment.Payment payment : payments) {
                    LocalDate paymentDate = payment.getDate();

                    log.info("Processing payment - invoiceDate: {}, paymentDate: {}", invoiceDate, paymentDate);

                    if (invoiceDate != null && paymentDate != null) {
                        long days = ChronoUnit.DAYS.between(invoiceDate, paymentDate);
                        log.info("Days between invoice and payment: {}", days);
                        totalDays += Math.max(0, days); // Avoid negative values
                        count++;
                    }
                }
            }

            double result = count > 0 ? (double) totalDays / count : 0.0;
            log.info("Final calculation: totalDays={}, count={}, result={}", totalDays, count, result);
            return result;
        } catch (Exception e) {
            log.error("Exception in calculateAveragePaymentDuration", e);
            return 0.0;
        }
    }

    /**
     * Calculates risk level based on overdue invoices and payment patterns.
     * @param client The client to evaluate
     * @param dateRange Date range for calculations
     * @return Risk level enum
     */
    public RiskLevel calculateRiskLevel(Client client, @Nullable LocalDateRange dateRange) {
        try {
            if (dateRange == null) {
                return RiskLevel.LOW;
            }

            Long overdueCount = dataManager.loadValue("SELECT COUNT(i) FROM Invoice i " +
                    "WHERE i.client.id = :clientId AND i.date BETWEEN :fromDate AND :toDate " +
                    "AND i.status = :overdueStatus", Long.class)
                    .parameter("clientId", client.getId())
                    .parameter("fromDate", dateRange.startDate())
                    .parameter("toDate", dateRange.endDate())
                    .parameter("overdueStatus", InvoiceStatus.OVERDUE.getId())
                    .one();

            BigDecimal overdueAmount = dataManager.loadValue("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i " +
                    "WHERE i.client.id = :clientId AND i.date BETWEEN :fromDate AND :toDate " +
                    "AND i.status = :overdueStatus", BigDecimal.class)
                    .parameter("clientId", client.getId())
                    .parameter("fromDate", dateRange.startDate())
                    .parameter("toDate", dateRange.endDate())
                    .parameter("overdueStatus", InvoiceStatus.OVERDUE.getId())
                    .one();

            double avgPaymentDuration = calculateAveragePaymentDuration(client, dateRange);

            int overdueCountInt = overdueCount != null ? overdueCount.intValue() : 0;
            BigDecimal overdueAmountSafe = overdueAmount != null ? overdueAmount : BigDecimal.ZERO;

            if (overdueCountInt >= HIGH_RISK_OVERDUE_COUNT ||
                    overdueAmountSafe.compareTo(HIGH_RISK_OVERDUE_AMOUNT) > 0 ||
                    avgPaymentDuration > HIGH_RISK_PAYMENT_DURATION) {
                return RiskLevel.HIGH;
            } else if (overdueCountInt >= MEDIUM_RISK_OVERDUE_COUNT ||
                    overdueAmountSafe.compareTo(MEDIUM_RISK_OVERDUE_AMOUNT) > 0 ||
                    avgPaymentDuration > MEDIUM_RISK_PAYMENT_DURATION) {
                return RiskLevel.MEDIUM;
            } else {
                return RiskLevel.LOW;
            }
        } catch (Exception e) {
            return RiskLevel.LOW;
        }
    }


    /**
     * Gets customer tenure as human-readable string.
     * @param client The client to evaluate
     * @return Tenure description (e.g., "2 years", "5 months", "New Customer")
     */
    public String getCustomerTenure(Client client) {
        try {
            if (client.getCreatedDate() == null) {
                return "Unknown";
            }

            LocalDate createdDate = client.getCreatedDate().toLocalDate();
            long daysSinceCreation = ChronoUnit.DAYS.between(createdDate, LocalDate.now());

            if (daysSinceCreation < 30) {
                return "New Customer";
            } else if (daysSinceCreation < 365) {
                long months = daysSinceCreation / 30;
                return months + " month" + (months == 1 ? "" : "s");
            } else {
                long years = daysSinceCreation / 365;
                return years + " year" + (years == 1 ? "" : "s");
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Determines if client has sales opportunities based on activity and payment history.
     * @param client The client to evaluate
     * @param dateRange Date range for calculations
     * @return true if client has recent engagement but no recent orders and no payment issues
     */
    public boolean hasSalesOpportunity(Client client, @Nullable LocalDateRange dateRange) {
        try {
            LocalDate lastActivity = clientService.getLastTransactionDate(client);
            boolean hasRecentActivity = false;
            if (lastActivity != null) {
                long daysSinceActivity = ChronoUnit.DAYS.between(lastActivity, LocalDate.now());
                hasRecentActivity = daysSinceActivity <= 30;
            }

            if (hasRecentActivity && !hasPaymentIssues(client, dateRange)) {
                Long recentOrders = dataManager.loadValue("SELECT COUNT(o) FROM Order_ o " +
                        "WHERE o.client.id = :clientId AND o.date >= :recentDate", Long.class)
                        .parameter("clientId", client.getId())
                        .parameter("recentDate", LocalDate.now().minusDays(30))
                        .one();
                return (recentOrders == null || recentOrders == 0);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client is a long-term customer based on creation date.
     * @param client The client to evaluate
     * @return true if client was created more than LONG_TERM_CUSTOMER_DAYS (365 days) ago
     */
    public boolean isLongTermCustomer(Client client) {
        try {
            if (client.getCreatedDate() == null) {
                return false;
            }

            LocalDate createdDate = client.getCreatedDate().toLocalDate();
            long days = ChronoUnit.DAYS.between(createdDate, LocalDate.now());
            return days > ClientReportThresholds.LONG_TERM_CUSTOMER_DAYS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client has recent activity based on last transaction date.
     * @param client The client to evaluate
     * @return true if client had transactions within RECENT_ACTIVITY_DAYS (30 days)
     */
    public boolean hasRecentActivity(Client client) {
        try {
            LocalDate lastActivity = clientService.getLastTransactionDate(client);
            if (lastActivity == null) {
                return false;
            }

            long days = ChronoUnit.DAYS.between(lastActivity, LocalDate.now());
            return days <= ClientReportThresholds.RECENT_ACTIVITY_DAYS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client is of business type (not individual).
     * @param client The client to evaluate
     * @return true if client type is BUSINESS
     */
    public boolean isBusinessType(Client client) {
        try {
            return client.getType() != null && "BUSINESS".equals(client.getType().name());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a client has outstanding balance above the flag threshold.
     * @param client The client to evaluate
     * @return true if outstanding balance exceeds OUTSTANDING_BALANCE_FLAG (€1000)
     */
    public boolean hasOutstandingBalanceFlag(Client client) {
        try {
            BigDecimal outstanding = clientService.getOutstandingBalance(client);
            if (outstanding == null) {
                return false;
            }
            return outstanding.compareTo(ClientReportThresholds.OUTSTANDING_BALANCE_FLAG) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}