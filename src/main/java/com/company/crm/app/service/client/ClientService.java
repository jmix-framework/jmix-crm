package com.company.crm.app.service.client;

import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.order.Order;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanBuilder;
import io.jmix.core.FetchPlans;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
    public Map<ClientType, List<Client>> loadClientsByType() {
        Map<ClientType, List<Client>> clientsByType = new HashMap<>();
        clientRepository.findAll().forEach(client ->
                clientsByType.computeIfAbsent(client.getType(),
                        k -> new ArrayList<>()).add(client));
        return clientsByType;
    }

    /**
     * Retrieves a list of clients who have at least one associated order.
     */
    public List<Client> loadClientsWithOrders() {
        return clientRepository.findAllByOrdersIsNotEmpty(clientWithOrdersFetchPlan());
    }

    /**
     * Retrieves a list of clients who have at least one associated payment.
     */
    public List<Client> loadClientsWithPayments() {
        return clientRepository.findAllWithPayments(clientWithPaymentsFetchPlan());
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

    /**
     * Returns a map of the top buyers and their corresponding total purchase amounts,
     * sorted by total purchase in descending order.
     *
     * @param amount the maximum number of top buyers to include in the result.
     * @return a map where the keys are the {@link Client} and the values are the total purchase amounts.
     */
    public Map<Client, BigDecimal> getBestBuyers(int amount) {
        if (amount <= 0) {
            return new HashMap<>();
        }

        Map<Client, BigDecimal> totalsByClient = new HashMap<>();

        loadClientsWithOrders().forEach(client -> {
            BigDecimal total = client.getOrders().stream()
                    .map(Order::getTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalsByClient.put(client, total);
        });

        return totalsByClient.entrySet().stream()
                .limit(amount)
                .sorted(comparingByValue(Comparator.reverseOrder()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}
