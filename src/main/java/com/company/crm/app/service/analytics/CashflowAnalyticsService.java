package com.company.crm.app.service.analytics;

import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service: loads entities and delegates per-invoice assessment to domain policies/services.
 */
@Service("crm_CashflowAnalyticsService")
public class CashflowAnalyticsService {

    private static final String UNASSIGNED = CategoryAllocationPolicy.UNASSIGNED;
    private static final int MAX_CRITICAL_INVOICES = 10;

    private final DataManager dataManager;
    private final FetchPlans fetchPlans;
    private final InvoiceRiskAssessmentService invoiceRiskAssessmentService;

    public CashflowAnalyticsService(DataManager dataManager,
                                    FetchPlans fetchPlans,
                                    InvoiceRiskAssessmentService invoiceRiskAssessmentService) {
        this.dataManager = dataManager;
        this.fetchPlans = fetchPlans;
        this.invoiceRiskAssessmentService = invoiceRiskAssessmentService;
    }

    public List<CategoryRiskMetrics> assessCategoryCashflowRisk(
            LocalDate fromDate,
            LocalDate toDate,
            UUID clientId,
            LocalDate asOfDate) {
        return assessCategoryCashflowRiskReport(fromDate, toDate, clientId, true, asOfDate).riskByCategory();
    }

    public CategoryCashflowRiskAssessmentResult assessCategoryCashflowRiskReport(
            LocalDate fromDate,
            LocalDate toDate,
            UUID clientId,
            Boolean includePaid,
            LocalDate asOfDate) {
        LocalDate effectiveAsOfDate = asOfDate != null ? asOfDate : LocalDate.now();
        boolean effectiveIncludePaid = includePaid == null || includePaid;
        List<Invoice> invoices = loadInvoices(fromDate, toDate, clientId, effectiveIncludePaid);
        Map<UUID, List<OrderItem>> orderItemsByOrderId = loadOrderItemsByOrderIds(invoices);

        List<InvoiceRiskAssessmentService.InvoiceRiskAssessmentResult> invoiceAssessments = invoices.stream()
                .map(invoice -> assessInvoice(invoice, orderItemsByOrderId, effectiveAsOfDate))
                .toList();

        Map<String, CategoryAccumulator> accumulators = invoiceAssessments.stream()
                .collect(HashMap::new, this::mergeAssessment, this::mergeAccumulators);

        List<CategoryRiskMetrics> riskByCategory = accumulators.values().stream()
                .map(CategoryAccumulator::toMetrics)
                .sorted(Comparator.comparing(CategoryRiskMetrics::overdueOpenAmount).reversed()
                        .thenComparing(Comparator.comparing(
                                CategoryRiskMetrics::dtcDaysWeighted,
                                Comparator.nullsLast(Comparator.reverseOrder()))))
                .toList();

        List<CriticalInvoiceMetrics> criticalInvoices = invoiceAssessments.stream()
                .flatMap(assessment -> assessment.criticalInvoices().stream())
                .sorted(Comparator
                        .comparing(CriticalInvoiceMetrics::categoryOpenAmount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comparator.comparingLong(CriticalInvoiceMetrics::daysOverdue).reversed())
                        .thenComparing(CriticalInvoiceMetrics::invoiceNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_CRITICAL_INVOICES)
                .toList();

        return new CategoryCashflowRiskAssessmentResult(riskByCategory, criticalInvoices);
    }

    private InvoiceRiskAssessmentService.InvoiceRiskAssessmentResult assessInvoice(
            Invoice invoice,
            Map<UUID, List<OrderItem>> orderItemsByOrderId,
            LocalDate asOfDate) {
        UUID orderId = invoice.getOrder() != null ? invoice.getOrder().getId() : null;
        List<OrderItem> orderItems = orderId != null ? orderItemsByOrderId.getOrDefault(orderId, List.of()) : List.of();
        return invoiceRiskAssessmentService.assessInvoiceRisk(invoice, orderItems, asOfDate);
    }

    private void mergeAssessment(Map<String, CategoryAccumulator> accumulators,
                                 InvoiceRiskAssessmentService.InvoiceRiskAssessmentResult result) {
        result.categories().forEach(position ->
                accumulators.merge(
                        position.categoryCode(),
                        CategoryAccumulator.fromRiskPosition(position),
                        CategoryAccumulator::merge
                )
        );

        if (result.overpaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            accumulators.merge(
                    UNASSIGNED,
                    CategoryAccumulator.fromOverpayment(UNASSIGNED, UNASSIGNED, result.overpaymentAmount()),
                    CategoryAccumulator::merge
            );
        }
    }

    private void mergeAccumulators(Map<String, CategoryAccumulator> left, Map<String, CategoryAccumulator> right) {
        right.forEach((categoryCode, accumulator) ->
                left.merge(categoryCode, accumulator, CategoryAccumulator::merge));
    }

    private List<Invoice> loadInvoices(LocalDate fromDate, LocalDate toDate, UUID clientId, boolean includePaid) {
        FetchPlan fetchPlan = fetchPlans.builder(Invoice.class)
                .addFetchPlan(FetchPlan.BASE)
                .add("client", FetchPlan.BASE)
                .add("payments", FetchPlan.BASE)
                .add("order", FetchPlan.BASE)
                .build();

        StringBuilder query = new StringBuilder("select i from Invoice i where 1=1");
        Map<String, Object> params = new HashMap<>();

        if (fromDate != null) {
            query.append(" and i.date >= :fromDate");
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            query.append(" and i.date <= :toDate");
            params.put("toDate", toDate);
        }
        if (clientId != null) {
            query.append(" and i.client.id = :clientId");
            params.put("clientId", clientId);
        }
        if (!includePaid) {
            query.append(" and (i.status is null or i.status <> :paidStatus)");
            params.put("paidStatus", InvoiceStatus.PAID.getId());
        }

        var loader = dataManager.load(Invoice.class)
                .query(query.toString())
                .fetchPlan(fetchPlan);
        params.forEach(loader::parameter);
        return loader.list();
    }

    private Map<UUID, List<OrderItem>> loadOrderItemsByOrderIds(List<Invoice> invoices) {
        Set<UUID> orderIds = invoices.stream()
                .map(Invoice::getOrder)
                .filter(order -> order != null && order.getId() != null)
                .map(Order::getId)
                .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return Map.of();
        }

        FetchPlan fetchPlan = fetchPlans.builder(OrderItem.class)
                .addFetchPlan(FetchPlan.BASE)
                .add("order", FetchPlan.BASE)
                .add("categoryItem", categoryItem -> categoryItem
                        .addFetchPlan(FetchPlan.BASE)
                        .add("category", FetchPlan.BASE))
                .build();

        List<OrderItem> orderItems = dataManager.load(OrderItem.class)
                .query("select oi from OrderItem oi where oi.order.id in :orderIds")
                .parameter("orderIds", orderIds)
                .fetchPlan(fetchPlan)
                .list();

        return orderItems.stream()
                .collect(Collectors.groupingBy(orderItem -> orderItem.getOrder().getId()));
    }
}
