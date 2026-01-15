package com.company.crm.app.service.finance;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceRepository;
import com.company.crm.model.invoice.InvoiceStatus;
import io.jmix.data.Sequence;
import io.jmix.data.Sequences;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Service
public class InvoiceService {

    private static final Sequence ORDER_NUMBER_SEQUENCE =
            Sequence.withName("CRM_INVOICE_NUMBER").setStartValue(1000);

    private final Sequences sequences;
    private final InvoiceRepository invoiceRepository;

    public InvoiceService(Sequences sequences, InvoiceRepository invoiceRepository) {
        this.sequences = sequences;
        this.invoiceRepository = invoiceRepository;
    }

    public String getNextInvoiceNumber() {
        return "INV-" + sequences.createNextValue(ORDER_NUMBER_SEQUENCE);
    }

    /**
     * @see #getAllInvoicesCount(LocalDateRange)
     */
    public long getAllInvoicesCount() {
        return getAllInvoicesCount(null);
    }

    /**
     * Retrieves the total count of invoices across all statuses, optionally filtered by a specified date range.
     * If no date range is provided, it returns the count of all invoices regardless of their date.
     *
     * @param dateRange an optional date range to filter invoices; if {@code null}, no date range filtering is applied
     * @return the total count of invoices within the specified date range and across all statuses
     */
    public long getAllInvoicesCount(@Nullable LocalDateRange dateRange) {
        return getInvoicesCount(dateRange, InvoiceStatus.values());
    }

    /**
     * @see #getPaidInvoicesCount(LocalDateRange)
     */
    public long getPaidInvoicesCount() {
        return getPaidInvoicesCount(null);
    }

    /**
     * Retrieves the count of invoices with the status {@link InvoiceStatus#PAID} within the specified date range.
     * If no date range is provided, it returns the count of all paid invoices.
     *
     * @param dateRange an optional date range to filter invoices; if {@code null}, no date range filtering is applied.
     * @return the count of paid invoices within the specified date range, or zero if none are found.
     */
    public long getPaidInvoicesCount(@Nullable LocalDateRange dateRange) {
        return getInvoicesCount(dateRange, InvoiceStatus.PAID);
    }

    /**
     * @see #getOverdueInvoices(LocalDateRange, Integer)
     */
    public List<Invoice> getOverdueInvoices() {
        return getOverdueInvoices(null, null);
    }

    /**
     * Retrieves a list of overdue invoices.
     *
     * @return a list of invoices that are overdue
     */
    public List<Invoice> getOverdueInvoices(@Nullable LocalDateRange dateRange,
                                            @Nullable Integer limit) {
        var query = new StringBuilder("e.status = :status");

        if (dateRange != null) {
            query.append(" AND e.date >= :startDate AND e.date <= :endDate");
        }

        var loader = invoiceRepository.queryLoader(query.toString())
                .parameter("status", InvoiceStatus.OVERDUE);

        if (dateRange != null) {
            loader.parameter("startDate", dateRange.startDate())
                    .parameter("endDate", dateRange.endDate());
        }

        if (limit != null) {
            loader.maxResults(limit);
        }

        return loader.list();
    }

    /**
     * Retrieves the count of invoices filtered by status and date range using JPQL aggregation.
     * This method is lightweight and does not load invoice entities into memory.
     *
     * @param dateRange an optional date range to filter invoices; if null, no date range filtering is applied
     * @param status    the {@link InvoiceStatus} to filter by
     * @return the count of invoices matching the criteria, or zero if no invoices are found
     */
    public long getInvoicesCount(@Nullable LocalDateRange dateRange, InvoiceStatus... status) {
        StringBuilder query = new StringBuilder("select count(e) from Invoice e");
        List<String> conditions = new ArrayList<>();

        if (status != null && status.length > 0) {
            conditions.add("e.status in :status");
        }

        if (dateRange != null) {
            conditions.add("e.date >= :startDate");
            conditions.add("e.date <= :endDate");
        }

        if (!conditions.isEmpty()) {
            query.append(" where ").append(String.join(" and ", conditions));
        }

        var loader = invoiceRepository.fluentValueLoader(query.toString(), BigDecimal.class);

        if (status != null) {
            loader.parameter("status", asList(status));
        }

        if (dateRange != null) {
            loader.parameter("startDate", dateRange.startDate());
            loader.parameter("endDate", dateRange.endDate());
        }

        return loader.optional().orElse(BigDecimal.ZERO).longValue();
    }

    /**
     * @see #getInvoicesCountByStatus(LocalDateRange)
     */
    public Map<InvoiceStatus, Long> getInvoicesCountByStatus() {
        return getInvoicesCountByStatus(null);
    }

    /**
     * Retrieves the count of invoices grouped by their status using JPQL aggregation,
     * optionally filtered by date range.
     * This method is lightweight and does not load invoice entities into memory.
     *
     * @param dateRange an optional date range to filter invoices; if null, no date range filtering is applied
     * @return a map where the key is the {@link InvoiceStatus} and the value is the count of invoices with that status.
     */
    public Map<InvoiceStatus, Long> getInvoicesCountByStatus(@Nullable LocalDateRange dateRange) {
        Map<InvoiceStatus, Long> countsByStatus = new HashMap<>();
        var infos = getInvoicesByDateRangeInfos(dateRange);

        for (var info : infos) {
            countsByStatus.merge(info.getStatus(), info.getAmount(), Long::sum);
        }

        return countsByStatus;
    }

    public List<InvoicesByDateRangeInfo> getInvoicesByDateRangeInfos(@Nullable LocalDateRange dateRange) {
        StringBuilder query = new StringBuilder(
                "select e.date as invoiceDate, e.status as status, count(e) as amount " +
                        "from Invoice e ");

        if (dateRange != null) {
            query.append("where e.date >= :startDate and e.date <= :endDate ");
        }

        query.append("group by e.date, e.status order by e.date");

        var loader = invoiceRepository.fluentValuesLoader(query.toString())
                .properties("invoiceDate", "status", "amount");

        if (dateRange != null) {
            loader.parameter("startDate", dateRange.startDate());
            loader.parameter("endDate", dateRange.endDate());
        }

        return loader.list().stream().map(keyValue -> {
            Integer statusId = keyValue.getValue("status");
            Long count = keyValue.getValue("amount");
            InvoiceStatus status = InvoiceStatus.fromId(statusId);
            LocalDate date = keyValue.getValue("invoiceDate");
            return new InvoicesByDateRangeInfo(date, dateRange, status, count);
        }).toList();
    }
}
