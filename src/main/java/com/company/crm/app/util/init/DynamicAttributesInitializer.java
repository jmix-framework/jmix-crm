package com.company.crm.app.util.init;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.client.Client;
import io.jmix.core.Metadata;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.dynattr.AttributeType;
import io.jmix.dynattr.model.Category;
import io.jmix.dynattr.model.CategoryAttribute;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DynamicAttributesInitializer {

    private static final Logger log = LoggerFactory.getLogger(DynamicAttributesInitializer.class);

    private static final UUID SOFTWARE_PRODUCTS_CATEGORY_ID =
            UUID.fromString("019be668-493a-7793-b9a0-9cce6776212a");
    private static final UUID SALES_TERRITORY_CATEGORY_ID =
            UUID.fromString("019be66a-b715-70a7-b02c-776e96903ea8");

    private static final UUID SOFTWARE_PRODUCTS_VENDOR_ATTR_ID =
            UUID.fromString("019be669-5bb1-7580-adfa-58831e63b666");
    private static final UUID SALES_TERRITORY_SALES_AREA_ATTR_ID =
            UUID.fromString("019be66a-e48b-7996-9cfb-0ed1ddfed4c3");

    public static final String VENDOR_ATTRIBUTE_CODE = "softwareProductsVendor";
    public static final String VENDOR_ATTRIBUTE_NAME = "Vendor";
    public static final String SOFTWARE_PRODUCTS_CATEGORY_NAME = "Software products";

    public static final String SALES_TERRITORY_CATEGORY_NAME = "Sales territory";
    public static final String SALES_TERRITORY_SALES_AREA_NAME = "Sales Area";
    public static final String SALES_TERRITORY_SALES_AREA_CODE = "salesTerritorySalesArea";

    private final Metadata metadata;
    private final UnconstrainedDataManager dataManager;

    public DynamicAttributesInitializer(Metadata metadata, UnconstrainedDataManager dataManager) {
        this.metadata = metadata;
        this.dataManager = dataManager;
    }

    @PostConstruct
    public void createDynamicAttributesIfNeeded() {
        createSoftwareProductsCategoryIfNeeded();
        createSalesTerritoryCategoryIfNeeded();
    }

    private void createSoftwareProductsCategoryIfNeeded() {
        ensureCategoryWithAttribute(SOFTWARE_PRODUCTS_CATEGORY_ID,
                SOFTWARE_PRODUCTS_CATEGORY_NAME,
                CategoryItem.class.getSimpleName(),
                SOFTWARE_PRODUCTS_VENDOR_ATTR_ID,
                VENDOR_ATTRIBUTE_NAME,
                VENDOR_ATTRIBUTE_CODE);
    }

    private void createSalesTerritoryCategoryIfNeeded() {
        ensureCategoryWithAttribute(SALES_TERRITORY_CATEGORY_ID,
                SALES_TERRITORY_CATEGORY_NAME,
                Client.class.getSimpleName(),
                SALES_TERRITORY_SALES_AREA_ATTR_ID,
                SALES_TERRITORY_SALES_AREA_NAME,
                SALES_TERRITORY_SALES_AREA_CODE);
    }

    private void ensureCategoryWithAttribute(UUID categoryId, String categoryName, String entityType,
                                             UUID attributeId, String attributeName, String attributeCode) {
        log.info("Checking if dynamic attribute with category {} and code {} exists", categoryName, attributeName);
        Category category = findCategory(categoryId, categoryName, entityType)
                .orElseGet(() -> dataManager.save(createCategory(categoryId, categoryName, entityType)));

        if (findAttribute(attributeId, attributeCode).isPresent()) {
            log.info("Dynamic attribute with category {} and code {} already exists", categoryName, attributeCode);
            return;
        }

        log.info("Creating missing category {} with attribute {}", categoryName, attributeName);
        CategoryAttribute attribute = createAttribute(attributeId, category, entityType, attributeName, attributeCode);
        dataManager.save(attribute);
    }

    private Category createCategory(UUID id, String name, String entityType) {
        Category category = metadata.create(Category.class);
        category.setId(id);
        category.setName(name);
        category.setEntityType(entityType);
        category.setIsDefault(false);
        return category;
    }

    private CategoryAttribute createAttribute(UUID id, Category category, String entityType,
                                              String name, String code) {
        CategoryAttribute attribute = metadata.create(CategoryAttribute.class);
        attribute.setId(id);
        attribute.setCategory(category);
        attribute.setCategoryEntityType(entityType);
        attribute.setName(name);
        attribute.setCode(code);
        attribute.setDataType(AttributeType.STRING);
        attribute.setOrderNo(1);
        attribute.setRequired(false);
        attribute.setLookup(false);
        attribute.setIsCollection(false);

        var dynamicAttributesContainerId = "dynamicAttributes";
        if (SOFTWARE_PRODUCTS_VENDOR_ATTR_ID.equals(id)) {
            attribute.setTargetScreens(CrmConstants.ViewIds.CATEGORY_ITEM_DETAIL + "#" + dynamicAttributesContainerId);
        } else if (SALES_TERRITORY_SALES_AREA_ATTR_ID.equals(id)) {
            attribute.setTargetScreens(CrmConstants.ViewIds.CLIENT_DETAIL + "#" + dynamicAttributesContainerId);
        }

        return attribute;
    }

    private Optional<Category> findCategory(UUID id, String name, String entityType) {
        Optional<Category> byId = dataManager.load(Category.class).id(id).optional();

        if (byId.isPresent()) {
            return byId;
        }

        return dataManager.load(Category.class)
                .query("select c from dynat_Category c where c.name = :name and c.entityType = :entityType")
                .parameter("name", name)
                .parameter("entityType", entityType)
                .optional();
    }

    private Optional<CategoryAttribute> findAttribute(UUID id, String code) {
        Optional<CategoryAttribute> byId = dataManager.load(CategoryAttribute.class).id(id).optional();

        if (byId.isPresent()) {
            return byId;
        }

        return dataManager.load(CategoryAttribute.class)
                .query("select a from dynat_CategoryAttribute a where a.code = :code")
                .parameter("code", code)
                .optional();
    }
}
