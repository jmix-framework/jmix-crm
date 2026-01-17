package com.company.crm.ai.jmix.introspector;

import com.company.crm.AbstractTest;
import com.company.crm.ai.jmix.introspection.introspector.JpaDomainModelIntrospector;
import com.company.crm.ai.jmix.introspection.model.AiDomainModelDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiEntityDescriptor;
import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
import com.company.crm.ai.jmix.testmodel.OriginalTestEntity;
import com.company.crm.ai.jmix.testmodel.ReplacedTestEntity;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AI Domain Model Introspector with Jmix Data Model Extensions (@ReplaceEntity)
 */
class AiDomainModelExtensionTest extends AbstractTest {

    @Autowired
    private JpaDomainModelIntrospector introspector;

    @Autowired
    private MetadataTools metadataTools;

    @Autowired
    private Metadata metadata;

    @Test
    void shouldIntrospectOnlyReplacementEntity() {
        // Get both MetaClasses - the original and the replacement
        MetaClass originalMetaClass = metadata.getClass(OriginalTestEntity.class);
        MetaClass replacementMetaClass = metadata.getClass(ReplacedTestEntity.class);

        List<MetaClass> metaClasses = List.of(originalMetaClass, replacementMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        // Verify that only the replacement entity is present in the domain model
        // The @ReplaceEntity annotation should cause the original entity to be replaced
        assertThat(domainModel.entities()).hasSize(1);
        assertThat(domainModel.entities()).containsKey("ReplacedTestEntity");
        assertThat(domainModel.entities()).doesNotContainKey("OriginalTestEntity");

        // Verify the replacement entity properties
        AiEntityDescriptor replacedEntity = domainModel.entities().get("ReplacedTestEntity");
        // IMPORTANT: With @ReplaceEntity, the replacement entity keeps the original entity's caption/identity
        assertThat(replacedEntity.caption()).isEqualTo("OriginalTestEntity");
        assertThat(replacedEntity.comment()).isEqualTo("Replacement entity that extends the original with additional fields");

        Map<String, AiPropertyDescriptor> properties = replacedEntity.properties();

        // Check that replacement entity has inherited properties from original
        assertThat(properties).containsKey("originalName");
        AiPropertyDescriptor originalNameProperty = properties.get("originalName");
        assertThat(originalNameProperty.comment()).isEqualTo("Original name field");

        assertThat(properties).containsKey("originalValue");
        AiPropertyDescriptor originalValueProperty = properties.get("originalValue");
        assertThat(originalValueProperty.comment()).isEqualTo("Original value field");

        // Check additional field only available in replacement
        assertThat(properties).containsKey("additionalField");
        AiPropertyDescriptor additionalFieldProperty = properties.get("additionalField");
        assertThat(additionalFieldProperty.comment()).isEqualTo("Additional field only available in replacement");
    }

    @Test
    void shouldHandleReplacementInMixedEntitySet() {
        // Test with a mix of normal entities and replaced entities
        MetaClass originalMetaClass = metadata.getClass(OriginalTestEntity.class);
        MetaClass replacementMetaClass = metadata.getClass(ReplacedTestEntity.class);

        // Include other test entities as well
        MetaClass customerMetaClass = metadata.getClass(com.company.crm.ai.jmix.testmodel.CustomerTestEntity.class);

        List<MetaClass> metaClasses = List.of(originalMetaClass, replacementMetaClass, customerMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        // Should have 2 entities: ReplacedTestEntity and CustomerTestEntity
        // OriginalTestEntity should be replaced by ReplacedTestEntity
        assertThat(domainModel.entities()).hasSize(2);
        assertThat(domainModel.entities()).containsKeys("ReplacedTestEntity", "CustomerTestEntity");
        assertThat(domainModel.entities()).doesNotContainKey("OriginalTestEntity");

        // Verify both entities are properly introspected
        AiEntityDescriptor replacedEntity = domainModel.entities().get("ReplacedTestEntity");
        assertThat(replacedEntity.comment()).isEqualTo("Replacement entity that extends the original with additional fields");

        AiEntityDescriptor customerEntity = domainModel.entities().get("CustomerTestEntity");
        assertThat(customerEntity.comment()).isEqualTo("Test customer entity for domain model export testing");
    }

    @Test
    void shouldIntrospectReplacementEntityDirectly() {
        // Test introspecting only the replacement entity (without the original)
        MetaClass replacementMetaClass = metadata.getClass(ReplacedTestEntity.class);
        List<MetaClass> metaClasses = List.of(replacementMetaClass);

        AiDomainModelDescriptor domainModel = introspector.introspect(metaClasses);

        // Should have only the replacement entity
        assertThat(domainModel.entities()).hasSize(1);
        assertThat(domainModel.entities()).containsKey("ReplacedTestEntity");

        AiEntityDescriptor replacedEntity = domainModel.entities().get("ReplacedTestEntity");
        assertThat(replacedEntity.caption()).isEqualTo("OriginalTestEntity");

        // Verify it has the expected properties (inherited + additional)
        Map<String, AiPropertyDescriptor> properties = replacedEntity.properties();
        assertThat(properties).containsKeys("originalName", "originalValue", "additionalField");
    }
}