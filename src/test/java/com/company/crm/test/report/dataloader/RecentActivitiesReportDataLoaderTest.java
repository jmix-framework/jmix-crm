package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.report.config.ClientReportThresholds;
import com.company.crm.report.dataloader.RecentActivitiesReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecentActivitiesReportDataLoaderTest extends AbstractTest {

    @Autowired
    private RecentActivitiesReportDataLoader dataLoader;

    @Test
    void testLoadDataWithValidClient() {
        // given
        Client client = entities.client("Activity Client");
        ClientUserActivity first = dataManager.create(ClientUserActivity.class);
        first.setClient(client);
        first.setActionDescription("Called customer to discuss renewal");
        first.setCreatedDate(OffsetDateTime.now().minusHours(1));

        ClientUserActivity second = dataManager.create(ClientUserActivity.class);
        second.setClient(client);
        second.setActionDescription("Sent updated proposal");
        second.setCreatedDate(OffsetDateTime.now().minusHours(2));
        dataManager.save(first, second);

        Map<String, Object> params = Map.of("client", client);

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .allSatisfy(activity -> assertThat(activity).containsKeys("description", "createdDate", "createdDateFormatted", "user"));
        assertThat(result.stream().map(row -> row.get("description")).toList())
                .containsExactlyInAnyOrder("Called customer to discuss renewal", "Sent updated proposal");
    }

    @Test
    void testLoadDataWithNullClient() {
        // given
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("client", null);

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void testLoadDataReturnsEmptyListForNewClient() {
        // given
        Client client = entities.client("New Client");
        Map<String, Object> params = Map.of("client", client);

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void testLoadDataUsesThresholds() {
        // given
        Client client = entities.client("Threshold Client");
        for (int day = 0; day < 5; day++) {
            for (int index = 0; index < ClientReportThresholds.ACTIVITIES_PER_DAY; index++) {
                ClientUserActivity activity = dataManager.create(ClientUserActivity.class);
                activity.setClient(client);
                activity.setActionDescription("Threshold Activity d" + day + "-" + index);
                activity.setCreatedDate(OffsetDateTime.now().minusDays(day).minusMinutes(index));
                dataManager.save(activity);
            }
        }

        Map<String, Object> params = Map.of("client", client);

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(ClientReportThresholds.ACTIVITIES_PER_DAY);
        assertThat(result.stream().map(row -> row.get("description")).toList())
                .allSatisfy(description -> assertThat(description.toString()).startsWith("Threshold Activity d4-"));
    }

    @Test
    void testLoadDataFiltersEmptyDescriptions() {
        // given
        Client client = entities.client("Filter Client");
        ClientUserActivity emptyDescription = dataManager.create(ClientUserActivity.class);
        emptyDescription.setClient(client);
        emptyDescription.setActionDescription("   ");
        emptyDescription.setCreatedDate(OffsetDateTime.now().minusHours(1));

        ClientUserActivity emptyStringDescription = dataManager.create(ClientUserActivity.class);
        emptyStringDescription.setClient(client);
        emptyStringDescription.setActionDescription("");
        emptyStringDescription.setCreatedDate(OffsetDateTime.now().minusHours(2));
        dataManager.save(emptyDescription, emptyStringDescription);

        Map<String, Object> params = Map.of("client", client);

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(row -> row.get("description")).toList())
                .contains("Customer interaction recorded");
        assertThat(result)
                .allSatisfy(activity -> assertThat(activity.get("description").toString().trim()).isNotEmpty());
    }

    @Test
    void testLoadDataStructureConsistency() {
        // given
        Client client1 = entities.client("Client 1");
        Client client2 = entities.client("Client 2");

        ClientUserActivity client1Activity = dataManager.create(ClientUserActivity.class);
        client1Activity.setClient(client1);
        client1Activity.setActionDescription("Client 1 Activity");
        client1Activity.setCreatedDate(OffsetDateTime.now().minusHours(1));

        ClientUserActivity client2Activity = dataManager.create(ClientUserActivity.class);
        client2Activity.setClient(client2);
        client2Activity.setActionDescription("Client 2 Activity");
        client2Activity.setCreatedDate(OffsetDateTime.now().minusHours(1));
        dataManager.save(client1Activity, client2Activity);

        Map<String, Object> params1 = Map.of("client", client1);
        Map<String, Object> params2 = Map.of("client", client2);

        // when
        List<Map<String, Object>> result1 = dataLoader.loadData(null, null, params1);
        List<Map<String, Object>> result2 = dataLoader.loadData(null, null, params2);

        // then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        assertThat(result1.getFirst().keySet()).isEqualTo(result2.getFirst().keySet());
        assertThat(result1.getFirst().get("description")).isEqualTo("Client 1 Activity");
        assertThat(result2.getFirst().get("description")).isEqualTo("Client 2 Activity");
    }
}
