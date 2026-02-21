package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.report.dataloader.ContactsReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContactsReportDataLoaderTest extends AbstractTest {

    @Autowired
    private ContactsReportDataLoader dataLoader;

    @Test
    void testLoadDataWithActiveContacts() {
        // Given
        Client client = entities.client("Contact Client");
        dataManager.save(client);

        Contact activeContact1 = entities.contact(client, "John Doe", "Manager");
        activeContact1.setPhone("+1234567890");
        activeContact1.setEmail("john.doe@example.com");
        activeContact1.setStartDate(LocalDate.of(2023, 1, 15));

        Contact activeContact2 = entities.contact(client, "Jane Smith", "Director");
        activeContact2.setPhone("+1234567891");
        activeContact2.setEmail("jane.smith@example.com");
        activeContact2.setStartDate(LocalDate.of(2023, 6, 1));

        // Contact with end date in the future (should be included)
        Contact futureEndContact = entities.contact(client, "Bob Wilson", "Consultant");
        futureEndContact.setStartDate(LocalDate.of(2023, 3, 1));
        futureEndContact.setEndDate(LocalDate.now().plusDays(30));

        // Contact with end date in the past (should be excluded)
        Contact pastEndContact = entities.contact(client, "Alice Brown", "Former Manager");
        pastEndContact.setStartDate(LocalDate.of(2022, 1, 1));
        pastEndContact.setEndDate(LocalDate.now().minusDays(30));

        dataManager.save(activeContact1, activeContact2, futureEndContact, pastEndContact);

        Map<String, Object> params = createParams(client);

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(3);

        // Should be ordered by startDate DESC (newest first)
        assertThat(result.get(0).get("person")).isEqualTo("Jane Smith");
        assertThat(result.get(1).get("person")).isEqualTo("Bob Wilson");
        assertThat(result.get(2).get("person")).isEqualTo("John Doe");

        // Check all fields are present
        assertThat(result.get(0)).containsKeys("person", "position", "phone", "email", "startDate", "endDate");
    }

    @Test
    void testLoadDataWithNoContacts() {
        // Given
        Client client = entities.client("No Contacts Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client);

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testLoadDataWithNullClient() {
        // Given
        Map<String, Object> params = createParams(null);

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testLoadDataFiltersByClient() {
        // Given
        Client client1 = entities.client("Client 1");
        Client client2 = entities.client("Client 2");
        dataManager.save(client1, client2);

        Contact contact1 = entities.contact(client1, "Contact 1", "Manager");
        Contact contact2 = entities.contact(client2, "Contact 2", "Director");
        dataManager.save(contact1, contact2);

        Map<String, Object> params = createParams(client1);

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("person")).isEqualTo("Contact 1");
    }

    @Test
    void testLoadDataWithNullValues() {
        // Given
        Client client = entities.client("Null Values Client");
        dataManager.save(client);

        Contact contact = entities.contact(client, "Basic Contact", "Position");
        contact.setPhone(null);
        contact.setEmail(null);
        contact.setEndDate(null);
        dataManager.save(contact);

        Map<String, Object> params = createParams(client);

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> contactData = result.get(0);

        assertThat(contactData.get("person")).isEqualTo("Basic Contact");
        assertThat(contactData.get("position")).isEqualTo("Position");
        assertThat(contactData).containsKey("phone");
        assertThat(contactData).containsKey("email");
        assertThat(contactData).containsKey("endDate");
    }

    private Map<String, Object> createParams(Client client) {
        Map<String, Object> params = new HashMap<>();
        params.put("client", client);
        return params;
    }
}