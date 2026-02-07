package com.company.crm.test.user;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.user.UserService;
import com.company.crm.model.client.Client;
import com.company.crm.model.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest extends AbstractServiceTest<UserService> {

    @Test
    void loadAccountManagers_returnsDistinctUsers() {
        User manager1 = entities.user("manager-one");
        User manager2 = entities.user("manager-two");

        Client client1 = entities.client("Client 1");
        client1.setAccountManager(manager1);
        saveWithoutReload(client1);

        Client client2 = entities.client("Client 2");
        client2.setAccountManager(manager1);
        saveWithoutReload(client2);

        Client client3 = entities.client("Client 3");
        client3.setAccountManager(manager2);
        saveWithoutReload(client3);

        var managers = service.loadAccountManagers();

        assertThat(managers).containsExactlyInAnyOrder(manager1, manager2);
    }
}
