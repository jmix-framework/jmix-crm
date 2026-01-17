package com.company.crm.ai.jmix;

import com.company.crm.ai.jmix.introspection.AiDomainModelDescriptorYamlWriter;
import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiEntityDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiDomainModelDescriptor;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for AiDomainModelDescriptorYamlWriter
 */
class AiDomainModelDescriptorYamlWriterTest {

    private final AiDomainModelDescriptorYamlWriter writer = new AiDomainModelDescriptorYamlWriter();

    @Test
    void shouldWritePropertiesStructure() {
        Map<String, AiPropertyDescriptor> properties = new LinkedHashMap<>();

        // Add a datatype property
        properties.put("name", AiPropertyDescriptor.dataProperty("Localized caption", "Entity name", "String", null));

        // Add an enum property
        Map<String, Object> enumValues = Map.of("NEW", 10, "DONE", 20);
        properties.put("status", AiPropertyDescriptor.enumProperty(null, "Current status", "OrderStatus", enumValues, null));

        // Add a relation property
        properties.put("customer", AiPropertyDescriptor.relationProperty(null, "Customer reference", "MANY_TO_ONE", "Customer", "Customer", false, null));

        AiEntityDescriptor entity = new AiEntityDescriptor("Test entity caption", "Test entity", properties);
        AiDomainModelDescriptor model = new AiDomainModelDescriptor(Map.of("TestEntity", entity));

        String yaml = writer.writeToYaml(model);

        // Check structure
        assertThat(yaml).contains("TestEntity");
        assertThat(yaml).contains("properties:");

        // Check datatype property
        assertThat(yaml).contains("name:");
        assertThat(yaml).contains("javaType: String");
        assertThat(yaml).contains("type: datatype");

        // Check enum property
        assertThat(yaml).contains("status:");
        assertThat(yaml).contains("javaType: OrderStatus");
        assertThat(yaml).contains("type: enum");
        assertThat(yaml).contains("enumValues:");

        // Check relation property
        assertThat(yaml).contains("customer:");
        assertThat(yaml).contains("javaType: Customer");
        assertThat(yaml).contains("type: MANY_TO_ONE");
        assertThat(yaml).contains("target: Customer");
        assertThat(yaml).contains("optional: false");

        // Verify single properties section (no separate relations)
        assertThat(yaml).doesNotContain("relations:");
    }
}