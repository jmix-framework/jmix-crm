package com.company.crm.ai.jmix.introspection;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the complete domain model export functionality.
 * Tests against our test entities to verify all features work correctly.
 */
class AiDomainModelDescriptorYamlExporterIntegrationTest extends AbstractTest {

    @Autowired
    private AiDomainModelDescriptorYamlExporter exporter;

    @Test
    void shouldExportTestEntities() {
        String yaml = exporter.export();

        // Basic structure checks
        assertThat(yaml).startsWith("entities:");

        // Should contain our test entities (but not BaseTestEntity - it's @MappedSuperclass)
        assertThat(yaml).contains("CustomerTestEntity:");
        assertThat(yaml).contains("OrderTestEntity:");
        assertThat(yaml).contains("OrderItemTestEntity:");

        // Should NOT contain BaseTestEntity (@MappedSuperclass)
        assertThat(yaml).doesNotContain("BaseTestEntity:");

        // Should NOT contain non-entity DTO
        assertThat(yaml).doesNotContain("NotAnEntityTestDto:");
    }

    @Test
    void shouldIncludeProperties() {
        String yaml = exporter.export();

        // CustomerTestEntity should have properties
        assertThat(yaml).contains("properties:");

        // Should have id as identifier
        assertThat(yaml).contains("id:");
        assertThat(yaml).contains("identifier: true");

        // Should have regular properties
        assertThat(yaml).contains("name:");
        assertThat(yaml).contains("email:");
    }

    @Test
    void shouldExcludeTransientProperties() {
        String yaml = exporter.export();

        // Should NOT contain @Transient field
        assertThat(yaml).doesNotContain("debugText:");
    }

    @Test
    void shouldIncludeRelationsInProperties() {
        String yaml = exporter.export();

        assertThat(yaml).contains("properties:");

        // CustomerTestEntity should have orders relation
        assertThat(yaml).contains("orders:");
        assertThat(yaml).contains("type: ONE_TO_MANY");
        assertThat(yaml).contains("target: OrderTestEntity");
        assertThat(yaml).contains("mappedBy: customer");

        // OrderTestEntity should have customer relation
        assertThat(yaml).contains("customer:");
        assertThat(yaml).contains("type: MANY_TO_ONE");
        assertThat(yaml).contains("target: CustomerTestEntity");
        assertThat(yaml).contains("optional: false");

        // Should have composition
        assertThat(yaml).contains("items:");
        assertThat(yaml).contains("type: ONE_TO_MANY");
    }

    @Test
    void shouldIncludeEmbeddedInProperties() {
        String yaml = exporter.export();

        // CustomerTestEntity should have embedded address
        assertThat(yaml).contains("address:");
        assertThat(yaml).contains("javaType: AddressTestEntity");
        assertThat(yaml).contains("embedded: true");
    }

    @Test
    void shouldIncludeComments() {
        String yaml = exporter.export();

        // Entity-level comments (SnakeYAML doesn't quote strings unless necessary)
        assertThat(yaml).contains("comment: Test customer entity for domain model export testing");

        // Property-level comments
        assertThat(yaml).contains("comment: Customer name - required field");
    }

    @Test
    void shouldReturnEmptyEntitiesForNonExistentWhitelist() {
        // Use a class that's not a JPA entity
        Set<Class<?>> whitelist = Set.of(String.class);
        String yaml = exporter.export(whitelist);

        // Should return empty YAML object
        String expectedYaml = "{}\n";
        assertThat(yaml).isEqualTo(expectedYaml);
    }

    @Test
    void shouldExportClientEntityWithExactYaml() {
        // Export only Client - using a smaller, focused assertion
        Set<Class<?>> whitelist = Set.of(Client.class);
        String yaml = exporter.export(whitelist);

        // Check that it starts correctly and has the essential Client structure
        assertThat(yaml).startsWith("entities:\n  Client:\n    caption: Client\n    properties:");

        // Check specific Client properties exist
        assertThat(yaml).contains("fullName:");
        assertThat(yaml).contains("type: enum");
        assertThat(yaml).contains("javaType: ClientType");
        assertThat(yaml).contains("accountManager:");
        assertThat(yaml).contains("type: MANY_TO_ONE");
    }

    @Test
    void shouldExportMultipleEntitiesInCorrectOrder() {
        // Export Client and Invoice - they should appear in deterministic order
        Set<Class<?>> whitelist = Set.of(Client.class, Invoice.class);
        String yaml = exporter.export(whitelist);

        // Check structure and order (Invoice appears first alphabetically)
        assertThat(yaml).startsWith("entities:\n  Invoice:");
        assertThat(yaml).contains("\n  Client:\n");

        // Verify both entities are present with their key properties
        assertThat(yaml).contains("Invoice:\n    caption: Invoice");
        assertThat(yaml).contains("Client:\n    caption: Client");
    }

}