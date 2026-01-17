package com.company.crm.ai.jmix.introspector;

import com.company.crm.AbstractTest;
import com.company.crm.ai.jmix.introspection.introspector.JpaDomainModelIntrospector;
import com.company.crm.ai.jmix.introspection.model.AiDomainModelDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiEntityDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
import com.company.crm.ai.jmix.testmodel.CustomerTestEntity;
import com.company.crm.ai.jmix.testmodel.OrderTestEntity;
import com.company.crm.ai.jmix.testmodel.NotAnEntityTestDto;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AI Domain Model Introspector focusing on model structure validation
 */
class AiDomainModelIntrospectorTest extends AbstractTest {

    @Autowired
    private JpaDomainModelIntrospector introspector;

    @Autowired
    private MetadataTools metadataTools;

    @Autowired
    private Metadata metadata;

    @Test
    void shouldIntrospectClientWithCorrectProperties() {
        // Get Client MetaClass
        MetaClass clientMetaClass = metadata.getClass(Client.class);
        List<MetaClass> metaClasses = List.of(clientMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        // Verify single entity
        assertThat(domainModel.entities()).hasSize(1);
        assertThat(domainModel.entities()).containsKey("Client");

        AiEntityDescriptor clientEntity = domainModel.entities().get("Client");
        assertThat(clientEntity.caption()).isEqualTo("Client");

        // Check specific properties for data consistency
        Map<String, AiPropertyDescriptor> properties = clientEntity.properties();

        // Check ID property
        assertThat(properties).containsKey("id");
        AiPropertyDescriptor idProperty = properties.get("id");
        assertThat(idProperty.identifier()).isTrue();
        assertThat(idProperty.javaType()).isEqualTo("UUID");

        // Check enum property
        assertThat(properties).containsKey("type");
        AiPropertyDescriptor typeProperty = properties.get("type");
        assertThat(typeProperty.type()).isEqualTo("enum");
        assertThat(typeProperty.javaType()).isEqualTo("ClientType");
        assertThat(typeProperty.enumValues()).isNotNull();
        assertThat(typeProperty.enumValues()).containsKeys("BUSINESS", "INDIVIDUAL");

        // Check relation property
        assertThat(properties).containsKey("accountManager");
        AiPropertyDescriptor accountManagerProperty = properties.get("accountManager");
        assertThat(accountManagerProperty.type()).isEqualTo("MANY_TO_ONE");
        assertThat(accountManagerProperty.target()).isEqualTo("User");

        // Check embedded property
        assertThat(properties).containsKey("address");
        AiPropertyDescriptor addressProperty = properties.get("address");
        assertThat(addressProperty.type()).isEqualTo("embedded");
        assertThat(addressProperty.embedded()).isTrue();
    }

    @Test
    void shouldIntrospectEmptyCollectionCorrectly() {
        AiDomainModelDescriptor domainModel = introspector.introspect(List.of());

        assertThat(domainModel.entities()).isEmpty();
    }

    @Test
    void shouldIntrospectTestEntityWithComments() {
        // Test our test model entities which have explicit @Comment annotations
        MetaClass customerTestMetaClass = metadata.getClass(CustomerTestEntity.class);
        List<MetaClass> metaClasses = List.of(customerTestMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        assertThat(domainModel.entities()).hasSize(1);
        assertThat(domainModel.entities()).containsKey("CustomerTestEntity");

        AiEntityDescriptor customerEntity = domainModel.entities().get("CustomerTestEntity");

        // Check entity-level comment from @Comment annotation
        assertThat(customerEntity.comment()).isEqualTo("Test customer entity for domain model export testing");
        assertThat(customerEntity.caption()).isEqualTo("CustomerTestEntity");

        Map<String, AiPropertyDescriptor> properties = customerEntity.properties();

        // Check property with comment
        assertThat(properties).containsKey("name");
        AiPropertyDescriptor nameProperty = properties.get("name");
        assertThat(nameProperty.comment()).isEqualTo("Customer name - required field");
        assertThat(nameProperty.caption()).isEqualTo("CustomerTestEntity.name");

        // Check email property
        assertThat(properties).containsKey("email");
        AiPropertyDescriptor emailProperty = properties.get("email");
        assertThat(emailProperty.comment()).isEqualTo("Email address for customer communication");
        assertThat(emailProperty.caption()).isEqualTo("CustomerTestEntity.email");

        // Check embedded property with comment
        assertThat(properties).containsKey("address");
        AiPropertyDescriptor addressProperty = properties.get("address");
        assertThat(addressProperty.comment()).isEqualTo("Embedded address information");
        assertThat(addressProperty.type()).isEqualTo("embedded");
        assertThat(addressProperty.embedded()).isTrue();

        // Check relation property with comment
        assertThat(properties).containsKey("orders");
        AiPropertyDescriptor ordersProperty = properties.get("orders");
        assertThat(ordersProperty.comment()).isEqualTo("List of orders placed by this customer");
        assertThat(ordersProperty.type()).isEqualTo("ONE_TO_MANY");
        assertThat(ordersProperty.target()).isEqualTo("OrderTestEntity");
        assertThat(ordersProperty.mappedBy()).isEqualTo("customer");

        // Verify transient fields are NOT included
        assertThat(properties).doesNotContainKey("debugText");
        assertThat(properties).doesNotContainKey("runtimeCache");
    }

    @Test
    void shouldIntrospectOrderTestEntityWithRelations() {
        MetaClass orderTestMetaClass = metadata.getClass(OrderTestEntity.class);
        List<MetaClass> metaClasses = List.of(orderTestMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        AiEntityDescriptor orderEntity = domainModel.entities().get("OrderTestEntity");
        Map<String, AiPropertyDescriptor> properties = orderEntity.properties();

        // Check ManyToOne relation
        assertThat(properties).containsKey("customer");
        AiPropertyDescriptor customerProperty = properties.get("customer");
        assertThat(customerProperty.type()).isEqualTo("MANY_TO_ONE");
        assertThat(customerProperty.target()).isEqualTo("CustomerTestEntity");
        assertThat(customerProperty.optional()).isFalse(); // required relation

        // Check OneToMany composition
        assertThat(properties).containsKey("items");
        AiPropertyDescriptor itemsProperty = properties.get("items");
        assertThat(itemsProperty.type()).isEqualTo("ONE_TO_MANY");
        assertThat(itemsProperty.target()).isEqualTo("OrderItemTestEntity");
        assertThat(itemsProperty.mappedBy()).isEqualTo("order");

        // Check enum property
        assertThat(properties).containsKey("status");
        AiPropertyDescriptor statusProperty = properties.get("status");
        assertThat(statusProperty.type()).isEqualTo("enum");
        assertThat(statusProperty.javaType()).isEqualTo("TestOrderStatus");
        assertThat(statusProperty.enumValues()).isNotNull();
        assertThat(statusProperty.enumValues()).containsKeys("DRAFT", "SUBMITTED", "APPROVED", "SHIPPED", "DELIVERED", "CANCELLED");
    }

    @Test
    void shouldIntrospectMultipleEntitiesCorrectly() {
        // Test multiple entities at once
        MetaClass clientMetaClass = metadata.getClass(Client.class);
        MetaClass invoiceMetaClass = metadata.getClass(Invoice.class);
        List<MetaClass> metaClasses = List.of(clientMetaClass, invoiceMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        assertThat(domainModel.entities()).hasSize(2);
        assertThat(domainModel.entities()).containsKeys("Client", "Invoice");

        // Verify both entities are properly introspected
        AiEntityDescriptor clientEntity = domainModel.entities().get("Client");
        assertThat(clientEntity.caption()).isEqualTo("Client");
        assertThat(clientEntity.properties()).containsKey("fullName");

        AiEntityDescriptor invoiceEntity = domainModel.entities().get("Invoice");
        assertThat(invoiceEntity.caption()).isEqualTo("Invoice");
        assertThat(invoiceEntity.properties()).containsKey("total");
        assertThat(invoiceEntity.properties()).containsKey("status");
    }

    @Test
    void shouldHandleEnumDescriptionsCorrectly() {
        MetaClass invoiceMetaClass = metadata.getClass(Invoice.class);
        List<MetaClass> metaClasses = List.of(invoiceMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        AiEntityDescriptor invoiceEntity = domainModel.entities().get("Invoice");
        AiPropertyDescriptor statusProperty = invoiceEntity.properties().get("status");

        // Check enum values and descriptions
        assertThat(statusProperty.enumValues()).isNotNull();
        assertThat(statusProperty.enumDescriptions()).isNotNull();

        // InvoiceStatus should have localized descriptions
        Map<String, String> descriptions = statusProperty.enumDescriptions();
        assertThat(descriptions).containsKey("NEW");
        assertThat(descriptions).containsKey("PENDING");
        assertThat(descriptions.get("NEW")).isEqualTo("New");
        assertThat(descriptions.get("PENDING")).isEqualTo("Pending");
    }

    @Test
    void shouldHandleAllPropertyTypesCorrectly() {
        MetaClass customerTestMetaClass = metadata.getClass(CustomerTestEntity.class);
        List<MetaClass> metaClasses = List.of(customerTestMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        Map<String, AiPropertyDescriptor> properties = domainModel.entities().get("CustomerTestEntity").properties();

        // Datatype properties
        AiPropertyDescriptor nameProperty = properties.get("name");
        assertThat(nameProperty.type()).isEqualTo("datatype");
        assertThat(nameProperty.javaType()).isEqualTo("String");
        assertThat(nameProperty.identifier()).isNull(); // not an ID

        // ID property
        AiPropertyDescriptor idProperty = properties.get("id");
        assertThat(idProperty.identifier()).isTrue();
        assertThat(idProperty.type()).isEqualTo("datatype");

        // Embedded property
        AiPropertyDescriptor addressProperty = properties.get("address");
        assertThat(addressProperty.type()).isEqualTo("embedded");
        assertThat(addressProperty.embedded()).isTrue();
        assertThat(addressProperty.javaType()).isEqualTo("AddressTestEntity");

        // One-to-many relation
        AiPropertyDescriptor ordersProperty = properties.get("orders");
        assertThat(ordersProperty.type()).isEqualTo("ONE_TO_MANY");
        assertThat(ordersProperty.target()).isEqualTo("OrderTestEntity");
        assertThat(ordersProperty.mappedBy()).isEqualTo("customer");
    }

    @Test
    void shouldExcludeJmixDtoEntitiesFromIntrospection() {
        // NotAnEntityTestDto is a Jmix DTO entity (non-JPA) and should be excluded
        MetaClass customerTestMetaClass = metadata.getClass(CustomerTestEntity.class);
        MetaClass dtoMetaClass = metadata.getClass(NotAnEntityTestDto.class);

        // Verify both MetaClasses exist in metadata
        assertThat(customerTestMetaClass).isNotNull();
        assertThat(dtoMetaClass).isNotNull();

        // Check that customer is JPA entity but DTO is not
        assertThat(metadataTools.isJpaEntity(customerTestMetaClass)).isTrue();
        assertThat(metadataTools.isJpaEntity(dtoMetaClass)).isFalse();

        // Introspect both classes together
        List<MetaClass> metaClasses = List.of(customerTestMetaClass, dtoMetaClass);
        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        // Verify only the JPA entity is included
        assertThat(domainModel.entities()).hasSize(1);
        assertThat(domainModel.entities()).containsKey("CustomerTestEntity");
        assertThat(domainModel.entities()).doesNotContainKey("NotAnEntityTestDto");

        // Verify the DTO entity is properly a Jmix entity but excluded from domain model
        AiEntityDescriptor customerEntity = domainModel.entities().get("CustomerTestEntity");
        assertThat(customerEntity).isNotNull();
        assertThat(customerEntity.caption()).isEqualTo("CustomerTestEntity");
    }

    @Test
    void shouldIntrospectAllJpaEntitiesButExcludeDtoEntities() {
        // Test the main introspect() method that gets all JPA entities
        AiDomainModelDescriptor domainModel = introspector.introspect();

        // Verify that NotAnEntityTestDto is not included in the full introspection
        assertThat(domainModel.entities()).doesNotContainKey("NotAnEntityTestDto");

        // But verify that real JPA entities are included
        assertThat(domainModel.entities()).containsKey("Client");
        assertThat(domainModel.entities()).containsKey("Invoice");

        // Verify we have a reasonable number of entities (should be more than just test entities)
        assertThat(domainModel.entities()).hasSizeGreaterThan(5);
    }
}