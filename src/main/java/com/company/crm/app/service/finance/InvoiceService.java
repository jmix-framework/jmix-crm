package com.company.crm.app.service.finance;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceRepository;
import com.company.crm.model.invoice.InvoiceStatus;
import org.springframework.lang.Nullable;
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

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
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
        if (dateRange == null) {
            return invoiceRepository.queryLoader("e.dueDate > e.date").list();
        }
        var loader = invoiceRepository.queryLoader("e.dueDate > e.date and e.date >= :startDate and e.date <= :endDate")
                .parameter("startDate", dateRange.startDate())
                .parameter("endDate", dateRange.endDate());

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
    public Long getInvoicesCount(@Nullable LocalDateRange dateRange, InvoiceStatus... status) {
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
     * Retrieves the count of invoices grouped by their status using JPQL aggregation.
     * This method is lightweight and does not load invoice entities into memory.
     *
     * @return a map where the key is the {@link InvoiceStatus} and the value is the count of invoices with that status.
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

    /**
     * Groups invoices by their status and returns a map where the key is the {@link InvoiceStatus}
     * and the value is the list of invoices with that status.
     */
    public Map<InvoiceStatus, List<Invoice>> getInvoicesByStatus() {
        Map<InvoiceStatus, List<Invoice>> invoicesByStatus = new HashMap<>();
        invoiceRepository.findAll().forEach(invoice ->
                invoicesByStatus.computeIfAbsent(invoice.getStatus(),
                        i -> new ArrayList<>()).add(invoice));
        return invoicesByStatus;
    }
}
