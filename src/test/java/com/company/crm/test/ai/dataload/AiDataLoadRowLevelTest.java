package com.company.crm.test.ai.dataload;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.user.User;
import io.jmix.aitools.dataload.execution.JpqlExecutionRequest;
import io.jmix.aitools.dataload.execution.JpqlExecutionResult;
import io.jmix.aitools.dataload.execution.JpqlExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The query the CRM assistant writes runs with the rights of the current user: admin sees every client,
 * alice (Manager + Only My Accounts) only the clients she manages. No model is involved.
 */
class AiDataLoadRowLevelTest extends AbstractTest {

    @Autowired
    private JpqlExecutionService jpqlExecutionService;

    private static final String CLIENTS = "select c.name as clientName from Client c order by c.name";
    private static final String TOP_CLIENTS = "select c.name as clientName, sum(o.total) as ordersTotal "
            + "from Order_ o join o.client c group by c.name order by sum(o.total) desc";

    @Test
    void sameQueryReturnsOnlyOwnClientsForAlice() {
        User alice = testUsers.ensureUser("alice");
        Client aliceClient = client("Row Level Alice", alice);
        Client adminClient = client("Row Level Admin", testUsers.ensureUser("admin"));
        entities.order(aliceClient, LocalDate.now(), OrderStatus.NEW, new BigDecimal("100"));
        entities.order(adminClient, LocalDate.now(), OrderStatus.NEW, new BigDecimal("200"));

        assertThat(clientNamesAs("admin", CLIENTS)).contains("Row Level Alice", "Row Level Admin");
        assertThat(clientNamesAs("alice", CLIENTS)).contains("Row Level Alice").doesNotContain("Row Level Admin");
        // a joined entity gets its row-level condition from AI Tools ("Access conditions applied" in the log)
        assertThat(clientNamesAs("admin", TOP_CLIENTS)).containsSubsequence("Row Level Admin", "Row Level Alice");
        assertThat(clientNamesAs("alice", TOP_CLIENTS)).contains("Row Level Alice").doesNotContain("Row Level Admin");
    }

    private List<Object> clientNamesAs(String username, String jpql) {
        JpqlExecutionResult result = withUser(username, () -> jpqlExecutionService.execute(new JpqlExecutionRequest(
                "Сколько у нас клиентов?", jpql, List.of(),
                jpql.contains("ordersTotal") ? List.of("clientName", "ordersTotal") : List.of("clientName"),
                null, null)));
        assertThat(result.getValidationResult().getIssues()).isEmpty();
        assertThat(result.getExecutionError()).isNull();
        return result.getRows().stream().map(row -> row.get("clientName")).toList();
    }

    private Client client(String name, User accountManager) {
        Client client = entities.client(name);
        client.setAccountManager(accountManager);
        return saveWithoutReload(client);
    }
}
