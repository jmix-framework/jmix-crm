package com.company.crm.test.init;

import com.company.crm.CRMApplication;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.init.DynamicAttributesInitializer;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.client.Client;
import io.jmix.core.Metadata;
import io.jmix.dynattr.DynAttrMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * First start of the local profile against a database Liquibase has not migrated yet (issue #52).
 * The private in-memory database is used by this context only, and the test itself writes nothing,
 * so there is no data to clean up.
 */
@ActiveProfiles({CrmConstants.SpringProfiles.TEST, CrmConstants.SpringProfiles.LOCAL})
@SpringBootTest(
        classes = CRMApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "main.datasource.url=jdbc:hsqldb:mem:dynamic-attributes-initializer-test",
                "crm.generateDemoData=false",
                "vaadin.launch-browser=false"
        })
class DynamicAttributesInitializerTest {

    @Autowired
    private Metadata metadata;
    @Autowired
    private DynAttrMetadata dynAttrMetadata;

    @Test
    void createsDynamicAttributesOnFirstStartWithEmptyDatabase() {
        assertThat(dynAttrMetadata.getAttributeByCode(metadata.getClass(CategoryItem.class),
                DynamicAttributesInitializer.VENDOR_ATTRIBUTE_CODE))
                .isPresent();
        assertThat(dynAttrMetadata.getAttributeByCode(metadata.getClass(Client.class),
                DynamicAttributesInitializer.SALES_TERRITORY_SALES_AREA_CODE))
                .isPresent();
    }
}
