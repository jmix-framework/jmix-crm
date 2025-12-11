package com.company.crm.app.service.client;

import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanBuilder;
import io.jmix.core.FetchPlans;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

@Service
public class ClientService {

    private final FetchPlans fetchPlans;
    private final ClientRepository clientRepository;

    public ClientService(FetchPlans fetchPlans, ClientRepository clientRepository) {
        this.fetchPlans = fetchPlans;
        this.clientRepository = clientRepository;
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
     * @param amount the maximum number of top buyers to include in the result.
     *               If {@param amount} is {@code null}, all available data will be returned
     * @return a map where the keys are the {@link Client} and the values are the total purchase amounts.
     */
    public Map<Client, BigDecimal> getBestBuyers(@Nullable Integer amount) {
        Map<Client, BigDecimal> totalsByClient = new LinkedHashMap<>();

        clientRepository.fluentValuesLoader(
                        "select distinct e.client as client, sum(e.total) as total " +
                                "from Order_ e " +
                                "group by e.client " +
                                "order by total desc " +
                                ((amount != null && amount > 0) ? ("limit " + amount) : ""))
                .properties("client", "total")
                .list().forEach(keyValue -> {
                    Client client = keyValue.getValue("client");
                    BigDecimal clientTotal = keyValue.getValue("total");
                    totalsByClient.merge(client, clientTotal, BigDecimal::add);
                });

        return totalsByClient;
    }

    /**
     * Calculates the total value of all orders associated with the specified client.
     *
     * @param client the {@link Client}s whose total order value is to be calculated.
     * @return the total value of all orders associated with the specified client as a {@link BigDecimal}.
     */
    public BigDecimal getOrdersTotalSum(Client...client) {
        return clientRepository.fluentValueLoader(
                "select sum(e.total) as total " +
                        "from Order_ e " +
                        "where e.client in :clients " +
                        "order by total desc", BigDecimal.class
        ).parameter("clients", asList(client)).optional().orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates the total value of all payments associated with the specified client.
     *
     * @param client the {@link Client}s whose total payments value is to be calculated.
     * @return the total value of all payments associated with the specified client as a {@link BigDecimal}.
     */
    public BigDecimal getPaymentsTotalSum(Client...client) {
        return clientRepository.fluentValueLoader(
                "select sum(p.amount) as total " +
                        "from Payment p " +
                        "where p.invoice.client in :client", BigDecimal.class
        ).parameter("client", asList(client)).optional().orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates the average bill (order value) for the specified client.
     *
     * @param client the {@link Client}s whose average bill is to be calculated.
     * @return the average bill for the specified client as a {@link BigDecimal}.
     */
    public BigDecimal getAverageBill(Client...client) {
        return clientRepository.fluentValueLoader(
                "select avg(e.total) as average " +
                        "from Order_ e " +
                        "where e.client in :client", BigDecimal.class
        ).parameter("client", asList(client)).optional().orElse(BigDecimal.ZERO);
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
