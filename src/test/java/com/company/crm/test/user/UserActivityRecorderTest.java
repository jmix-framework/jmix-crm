package com.company.crm.test.user;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.user.User;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.security.ManagerRole;
import com.company.crm.util.TestUsers;
import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserActivityRecorderTest extends AbstractTest {

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Autowired
    private TestUsers testUsers;

    @Test
    void creatingClientByUser_createsActivity() {
        User user = testUsers.ensureUser("activity-recorder-user");
        testUsers.assignRole(user.getUsername(), ManagerRole.CODE);

        Client client = dataManager.create(Client.class);
        client.setName("Recorder Client");
        client.setAddress(entities.address());

        systemAuthenticator.runWithUser(user.getUsername(), () -> dataManager.save(client));

        List<ClientUserActivity> activities = dataManager.load(ClientUserActivity.class)
                .query("e.client = ?1", client)
                .list();

        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getUser()).isEqualTo(user);
        assertThat(activities.get(0).getActionDescription()).contains("client added");
    }
}
