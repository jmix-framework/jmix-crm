package com.company.crm.test.report.mapper;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.report.mapper.ReportContactMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportContactMapperTest extends AbstractTest {

    @Autowired
    private ReportContactMapper mapper;

    @Test
    void testToReportMapWithCompleteContact() {
        // given
        Client client = entities.client("Test Client");
        Contact contact = entities.contact(client, "John Doe", "Manager");
        contact.setPhone("+49 123 456789");
        contact.setEmail("john.doe@example.com");
        contact.setStartDate(LocalDate.of(2023, 1, 1));
        contact.setEndDate(null); // Active contact

        // when
        Map<String, Object> result = mapper.toReportMap(contact);

        // then
        assertThat(result).hasSize(9);
        assertThat(result.get("person")).isEqualTo("John Doe");
        assertThat(result.get("position")).isEqualTo("Manager");
        assertThat(result.get("phone")).isEqualTo("+49 123 456789");
        assertThat(result.get("email")).isEqualTo("john.doe@example.com");
        assertThat(result.get("startDate")).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(result.get("startDateFormatted")).isInstanceOf(String.class);
        assertThat((String) result.get("startDateFormatted")).isNotBlank();
        assertThat(result.get("endDate")).isNull();
        assertThat(result.get("endDateFormatted")).isEqualTo("");
        assertThat(result.get("isActive")).isEqualTo(true);
    }

    @Test
    void testToReportMapWithInactiveContact() {
        // given
        Client client = entities.client("Test Client");
        Contact contact = entities.contact(client, "Jane Smith", "Assistant");
        contact.setPhone("+49 987 654321");
        contact.setEmail("jane.smith@example.com");
        contact.setStartDate(LocalDate.of(2022, 6, 1));
        contact.setEndDate(LocalDate.of(2023, 12, 31)); // Inactive contact

        // when
        Map<String, Object> result = mapper.toReportMap(contact);

        // then
        assertThat(result).hasSize(9);
        assertThat(result.get("person")).isEqualTo("Jane Smith");
        assertThat(result.get("position")).isEqualTo("Assistant");
        assertThat(result.get("startDate")).isEqualTo(LocalDate.of(2022, 6, 1));
        assertThat(result.get("endDate")).isEqualTo(LocalDate.of(2023, 12, 31));
        assertThat(result.get("endDateFormatted")).isInstanceOf(String.class);
        assertThat((String) result.get("endDateFormatted")).isNotBlank();
        assertThat(result.get("isActive")).isEqualTo(false);
    }

    @Test
    void testToReportMapConsistency() {
        // given - Same contact mapped twice
        Client client = entities.client("Test Client");
        Contact contact = entities.contact(client, "Bob Wilson", "Director");
        contact.setPhone("+49 555 123456");
        contact.setEmail("bob.wilson@example.com");
        contact.setStartDate(LocalDate.of(2021, 3, 15));

        // when
        Map<String, Object> result1 = mapper.toReportMap(contact);
        Map<String, Object> result2 = mapper.toReportMap(contact);

        // then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.get("person")).isEqualTo("Bob Wilson");
        assertThat(result1.get("position")).isEqualTo("Director");
        assertThat(result1.get("isActive")).isEqualTo(true);
    }

    @Test
    void testToReportMapWithMissingPersonAndPosition_usesEmptyFallbacks() {
        // given
        Client client = entities.client("Edge Client");
        Contact contact = entities.contact(client, "Temp Person", "Temp Position");
        contact.setPerson(null);
        contact.setPosition(null);
        contact.setStartDate(LocalDate.of(2024, 1, 1));

        // when
        Map<String, Object> result = mapper.toReportMap(contact);

        // then
        assertThat(result.get("person")).isEqualTo("");
        assertThat(result.get("position")).isEqualTo("");
        assertThat(result.get("startDate")).isEqualTo(LocalDate.of(2024, 1, 1));
    }
}
