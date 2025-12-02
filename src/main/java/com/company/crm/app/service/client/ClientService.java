package com.company.crm.app.service.client;

import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderRepository;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientService {

    private final FetchPlans fetchPlans;
    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;

    public ClientService(OrderRepository orderRepository, FetchPlans fetchPlans, ClientRepository clientRepository) {
        this.fetchPlans = fetchPlans;
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
    }

    public Map<ClientType, List<Client>> getClientsByType() {
        Map<ClientType, List<Client>> clientsByType = new HashMap<>();
        clientRepository.findAll().forEach(client ->
                clientsByType.computeIfAbsent(client.getType(),
                        k -> new ArrayList<>()).add(client));
        return clientsByType;
    }

    public List<Client> getBestBuyers(int amount) {
        if (amount <= 0) {
            return new ArrayList<>();
        }

        Map<Client, BigDecimal> totalsByClient = new HashMap<>();

        orderRepository.findAll(0, amount,
                        fetchPlans.builder(Order.class)
                                .addFetchPlan(FetchPlan.BASE)
                                .add("client", FetchPlan.BASE)
                                .build())
                .forEach(order -> {
                    Client client = order.getClient();
                    if (client != null) {
                        BigDecimal total = order.getTotal();
                        if (total != null) {
                            totalsByClient.merge(client, total, BigDecimal::add);
                        }
                    }
                });

        return totalsByClient.entrySet().stream()
                .sorted(Map.Entry.<Client, BigDecimal>comparingByValue(Comparator.naturalOrder()).reversed())
                .map(Map.Entry::getKey)
                .toList();
    }
}
