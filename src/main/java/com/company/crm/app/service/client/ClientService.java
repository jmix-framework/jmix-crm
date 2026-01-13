package com.company.crm.app.service.client;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.order.OrderStatus;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanBuilder;
import io.jmix.core.FetchPlans;
import io.jmix.core.FluentValueLoader;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Service
public class ClientService {

    private final FetchPlans fetchPlans;
    private final ClientRepository clientRepository;

    public ClientService(FetchPlans fetchPlans, ClientRepository clientRepository) {
        this.fetchPlans = fetchPlans;
        this.clientRepository = clientRepository;
    }

    public List<CompletedOrdersByDateRangeInfo> getCompletedOrdersInfo(@Nullable LocalDateRange dateRange, Client... clients) {
        boolean clientsSpecified = clients.length > 0;

        StringBuilder query = new StringBuilder(
                "select e.date as orderDate, count(e) as amount, sum(e.total) as total " +
                        "from Order_ e ");

        List<String> conditions = new ArrayList<>();

        conditions.add("e.status = :status");

        if (clientsSpecified) {
            conditions.add("e.client in :clients");
        }

        if (dateRange != null) {
            conditions.add("e.date >= :startDate");
            conditions.add("e.date <= :endDate");
        }

        query.append("where ").append(String.join(" and ", conditions));
        query.append(" group by e.date order by e.date");

        var loader = clientRepository.fluentValuesLoader(query.toString())
                .properties("orderDate", "amount", "total")
                .parameter("status", OrderStatus.DONE);

        if (clientsSpecified) {
            loader.parameter("clients", asList(clients));
        }

        if (dateRange != null) {
            loader.parameter("startDate", dateRange.startDate())
                    .parameter("endDate", dateRange.endDate());
        }

        Long rangeTotalAmount = getCompletedOrdersAmount(dateRange, clients).longValue();
        var rangeTotalSum = ordersTotalSumLoader(new OrderStatus[]{OrderStatus.DONE}, clients)
                .optional().orElse(BigDecimal.ZERO);
        var salesCycleLength = getSalesCycleLength(dateRange, clients);

        return loader.list().stream().map(keyValue -> {
            LocalDate date = keyValue.getValue("orderDate");
            BigDecimal dateTotalSum = keyValue.getValue("total");
            Long dateAmount = keyValue.getValue("amount");
            return new CompletedOrdersByDateRangeInfo(date, dateRange, dateAmount,
                    dateTotalSum, rangeTotalAmount, rangeTotalSum, salesCycleLength);
        }).toList();
    }

    /**
     * Calculates the total number of completed orders based on the provided date range and clients.
     *
     * @param dateRange an optional date range to filter the orders; if null, no date range filtering is applied
     * @param clients   optional list of clients to filter the orders; if no clients are provided, all clients are considered
     * @return the total number of orders that match the specified criteria, or {@code BigDecimal.ZERO} if no orders are matched
     */
    public BigDecimal getCompletedOrdersAmount(@Nullable LocalDateRange dateRange,
                                               Client... clients) {
        boolean clientsSpecified = clients.length > 0;

        StringBuilder query = new StringBuilder("select count(e) from Order_ e");
        List<String> conditions = new ArrayList<>();

        conditions.add("e.status = :status");

        if (clientsSpecified) {
            conditions.add("e.client in :clients");
        }

        if (dateRange != null) {
            conditions.add("e.date >= :from");
            conditions.add("e.date <= :to");
        }

        query.append(" where ").append(String.join(" and ", conditions));

        var loader = clientRepository
                .fluentValueLoader(query.toString(), BigDecimal.class)
                .parameter("status", OrderStatus.DONE);

        if (clientsSpecified) {
            loader.parameter("clients", asList(clients));
        }

        if (dateRange != null) {
            loader.parameter("from", dateRange.startDate());
            loader.parameter("to", dateRange.endDate());
        }

        return loader.optional().orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates the average sales cycle length in days.
     * The sales cycle length is defined as the period between order creation and the final payment.
     * Only for completed orders.
     *
     * @param dateRange optional date range to filter orders. If {@code null}, no date filtering is applied.
     * @param clients   the clients whose orders should be considered. If empty, all clients are included.
     * @return the average sales cycle length in days as a {@link Integer}, or zero if no data is available.
     */
    public Integer getSalesCycleLength(@Nullable LocalDateRange dateRange,
                                       Client... clients) {
        boolean clientsSpecified = clients.length > 0;

        StringBuilder query = new StringBuilder(
                "select e.date as orderDate, max(p.date) as lastPaymentDate " +
                        "from Payment p " +
                        "join p.invoice inv " +
                        "join inv.order e ");

        List<String> conditions = new ArrayList<>();
        conditions.add("e.status = :status");
        conditions.add("e.date is not null");
        conditions.add("p.date is not null");

        if (clientsSpecified) {
            conditions.add("e.client in :clients");
        }

        if (dateRange != null) {
            conditions.add("e.date >= :startDate");
            conditions.add("e.date <= :endDate");
        }

        query.append("where ").append(String.join(" and ", conditions));
        query.append(" group by e.id, e.date");

        var loader = clientRepository.fluentValuesLoader(query.toString())
                .properties("orderDate", "lastPaymentDate")
                .parameter("status", OrderStatus.DONE);

        if (clientsSpecified) {
            loader.parameter("clients", asList(clients));
        }

        if (dateRange != null) {
            loader.parameter("startDate", dateRange.startDate());
            loader.parameter("endDate", dateRange.endDate());
        }

        long sumDays = 0;
        long ordersCount = 0;
        for (var keyValue : loader.list()) {
            LocalDate orderDate = keyValue.getValue("orderDate");
            LocalDate lastPaymentDate = keyValue.getValue("lastPaymentDate");
            if (orderDate == null || lastPaymentDate == null) {
                continue;
            }

            sumDays += ChronoUnit.DAYS.between(orderDate, lastPaymentDate);
            ordersCount++;
        }

        if (ordersCount == 0) {
            return 0;
        }

        return (int) (sumDays / ordersCount);
    }

    /**
     * Retrieves the count of clients grouped by their type using JPQL aggregation.
     * This method is lightweight and does not load client entities into memory.
     *
     * @return a map where the key is the {@link ClientType} and the value is the count of clients with that type.
     */
    public Map<ClientType, Long> getClientsCountByType() {
        Map<ClientType, Long> countsByType = new HashMap<>();

        clientRepository.fluentValuesLoader(
                        "select e.type as type, count(e) as amount " +
                                "from Client e " +
                                "group by e.type")
                .properties("type", "amount")
                .list()
                .forEach(keyValue -> {
                    ClientType type = keyValue.getValue("type");
                    Long count = keyValue.getValue("amount");
                    countsByType.put(type, count);
                });

        return countsByType;
    }

    /**
     * Groups clients by their type and returns a map where the key is the {@link ClientType}
     * and the value is a list of {@link Client} objects belonging to that type.
     */
    public Map<ClientType, List<Client>> getClientsByType() {
        Map<ClientType, List<Client>> clientsByType = new HashMap<>();
        clientRepository.findAll().forEach(client ->
                clientsByType.computeIfAbsent(client.getType(),
                        k -> new ArrayList<>()).add(client));
        return clientsByType;
    }

    /**
     * Retrieves a list of clients who have at least one associated order.
     */
    public List<Client> getClientsWithOrders() {
        return clientRepository.findAllByOrdersIsNotEmpty(clientWithOrdersFetchPlan());
    }

    /**
     * Retrieves a list of clients who have at least one associated payment.
     */
    public List<Client> getClientsWithPayments() {
        return clientRepository.findAllWithPayments(clientWithPaymentsFetchPlan());
    }

    /**
     * Returns a map of the top buyers and their corresponding total purchase amounts,
     * sorted by total purchase in descending order.
     *
     * @param limit the maximum number of top buyers to include in the result.
     *              If {@param limit} is {@code null}, all available data will be returned
     * @return a map where the keys are the {@link Client} and the values are the total purchase amounts.
     */
    public Map<Client, BigDecimal> getBestBuyers(@Nullable Integer limit) {
        return clientRepository.fluentValuesLoader(
                        "select distinct e.client as client, sum(e.total) as total " +
                                "from Order_ e " +
                                "group by e.client " +
                                "order by total desc ")
                .properties("client", "total")
                .maxResults(limit != null ? limit : 0)
                .list().stream()
                .collect(Collectors.toMap(
                        keyValue -> keyValue.getValue("client"),
                        keyValue -> keyValue.getValue("total")));
    }

    /**
     * Calculates the total value of all orders associated with the specified client.
     *
     * @param statuses the {@link OrderStatus}es to include in the total calculation.
     * @param client   the {@link Client}s whose total order value is to be calculated.
     * @return the total value of all orders associated with the specified client as a {@link BigDecimal}.
     */
    public BigDecimal getOrdersTotalSum(OrderStatus[] statuses, Client... client) {
        return ordersTotalSumLoader(statuses, client).optional().orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates the total value of all payments associated with the specified client.
     *
     * @param client the {@link Client}s whose total payments value is to be calculated.
     * @return the total value of all payments associated with the specified client as a {@link BigDecimal}.
     */
    public BigDecimal getPaymentsTotalSum(Client... client) {
        boolean clientSpecified = client.length > 0;

        var loader = clientRepository.fluentValueLoader(
                "select sum(p.amount) as total " +
                        "from Payment p " +
                        (clientSpecified ? "where p.invoice.client in :client" : ""),
                BigDecimal.class
        );

        if (clientSpecified) {
            loader.parameter("client", asList(client));
        }

        return loader.optional().orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates the average bill (order value) for the specified client.
     *
     * @param client the {@link Client}s whose average bill is to be calculated.
     * @return the average bill for the specified client as a {@link BigDecimal}.
     */
    public BigDecimal getAverageBill(Client... client) {
        return averageBillLoader(client).optional().orElse(BigDecimal.ZERO);
    }

    private FluentValueLoader<BigDecimal> ordersTotalSumLoader(@Nullable OrderStatus[] statuses, Client[] client) {
        boolean clientsSpecified = client.length > 0;
        boolean statusesSpecified = statuses != null && statuses.length > 0;

        StringBuilder query = new StringBuilder("select sum(e.total) from Order_ e");
        List<String> conditions = new ArrayList<>();

        if (clientsSpecified) {
            conditions.add("e.client in :clients");
        }

        if (statusesSpecified) {
            conditions.add("e.status in :statuses");
        }

        if (!conditions.isEmpty()) {
            query.append(" where ").append(String.join(" and ", conditions));
        }

        var loader = clientRepository.fluentValueLoader(query.toString(), BigDecimal.class);

        if (clientsSpecified) {
            loader.parameter("clients", asList(client));
        }

        if (statusesSpecified) {
            loader.parameter("statuses", asList(statuses));
        }

        return loader;
    }

    private FluentValueLoader<BigDecimal> averageBillLoader(Client... client) {
        boolean clientSpecified = client.length > 0;

        var loader = clientRepository.fluentValueLoader(
                "select avg(e.total) as average " +
                        "from Order_ e " +
                        (clientSpecified ? "where e.client in :client" : ""), BigDecimal.class
        );

        if (clientSpecified) {
            loader.parameter("client", asList(client));
        }
        return loader;
    }

    private FetchPlan clientWithOrdersFetchPlan() {
        return baseClientFetchPlanWith("orders", order -> order.addFetchPlan(FetchPlan.BASE));
    }

    private FetchPlan clientWithPaymentsFetchPlan() {
        return baseClientFetchPlanWith("invoices", invoice -> invoice.add("payments"));
    }

    private FetchPlan baseClientFetchPlanWith(String property,
                                              @Nullable Consumer<FetchPlanBuilder> propertyFetchPlan) {
        FetchPlanBuilder builder = baseClientFetchPlan();
        if (propertyFetchPlan != null) {
            builder.add(property, propertyFetchPlan);
        } else {
            builder.add(property);
        }
        return builder.build();
    }

    private FetchPlanBuilder baseClientFetchPlan() {
        return fetchPlans.builder(Client.class).addFetchPlan(FetchPlan.BASE);
    }
}
