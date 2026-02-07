package com.company.crm.test.user;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.user.UserActivityService;
import com.company.crm.model.client.Client;
import com.company.crm.model.user.User;
import com.company.crm.model.user.activity.UserActivity;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.model.user.activity.userprofile.UserProfileUserActivity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserActivityServiceTest extends AbstractServiceTest<UserActivityService> {

    @Test
    void loadActivities_returnsEntriesFromAllRepositories() {
        User user = entities.user("activity-user");
        Client client = entities.client("Activity Client");

        ClientUserActivity clientActivity = dataManager.create(ClientUserActivity.class);
        clientActivity.setClient(client);
        clientActivity.setUser(user);
        clientActivity.setActionDescription("Client activity");
        clientActivity.setCreatedDate(OffsetDateTime.now().minusHours(1));
        saveWithoutReload(clientActivity);

        UserProfileUserActivity profileActivity = dataManager.create(UserProfileUserActivity.class);
        profileActivity.setUser(user);
        profileActivity.setActionDescription("Profile activity");
        profileActivity.setCreatedDate(OffsetDateTime.now());
        saveWithoutReload(profileActivity);

        var activities = service.loadActivities(LocalDate.now(), 10);

        assertThat(activities)
                .extracting(UserActivity::getActionDescription)
                .containsExactly("Profile activity", "Client activity");
    }

    @Test
    void loadClientActivities_filtersByClientAndUser() {
        User user = entities.user("client-activity-user");
        Client client = entities.client("Client Filter");
        Client otherClient = entities.client("Client Other");

        ClientUserActivity activity1 = dataManager.create(ClientUserActivity.class);
        activity1.setClient(client);
        activity1.setUser(user);
        activity1.setActionDescription("Client activity 1");
        activity1.setCreatedDate(OffsetDateTime.now());
        saveWithoutReload(activity1);

        ClientUserActivity activity2 = dataManager.create(ClientUserActivity.class);
        activity2.setClient(otherClient);
        activity2.setUser(user);
        activity2.setActionDescription("Client activity 2");
        activity2.setCreatedDate(OffsetDateTime.now());
        saveWithoutReload(activity2);

        var activities = service.loadClientActivities(user, client, LocalDate.now(), 10);

        assertThat(activities)
                .extracting(ClientUserActivity::getActionDescription)
                .containsExactly("Client activity 1");
    }
}
