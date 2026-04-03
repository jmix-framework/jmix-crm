package com.company.crm.app.util.init;

import com.company.crm.app.config.SpringProfiles;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.client.Client;
import io.jmix.core.CoreProperties;
import io.jmix.core.LocaleResolver;
import io.jmix.core.Messages;
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

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    public static final String SOFTWARE_PRODUCTS_CATEGORY_NAME_KEY = "dynamicAttributes.softwareProducts.categoryName";
    public static final String SOFTWARE_PRODUCTS_VENDOR_ATTRIBUTE_NAME_KEY = "dynamicAttributes.softwareProducts.vendorName";
    public static final String SALES_TERRITORY_SALES_AREA_CODE = "salesTerritorySalesArea";
    public static final String SALES_TERRITORY_CATEGORY_NAME_KEY = "dynamicAttributes.salesTerritory.categoryName";
    public static final String SALES_TERRITORY_SALES_AREA_NAME_KEY = "dynamicAttributes.salesTerritory.salesAreaName";

    private final Metadata metadata;
    private final UnconstrainedDataManager dataManager;
    private final SpringProfiles springProfiles;
    private final Messages messages;
    private final CoreProperties coreProperties;

    public DynamicAttributesInitializer(Metadata metadata,
                                        UnconstrainedDataManager dataManager,
                                        SpringProfiles springProfiles,
                                        Messages messages,
                                        CoreProperties coreProperties) {
        this.metadata = metadata;
        this.dataManager = dataManager;
        this.springProfiles = springProfiles;
        this.messages = messages;
        this.coreProperties = coreProperties;
    }

    @PostConstruct
    public void postConstruct() {
        if (springProfiles.isLocalProfile()) {
            createDynamicAttributesIfNeeded();
        }
    }

    public void createDynamicAttributesIfNeeded() {
        createSoftwareProductsCategoryIfNeeded();
        createSalesTerritoryCategoryIfNeeded();
    }

    private void createSoftwareProductsCategoryIfNeeded() {
        String categoryName = getDefaultLocalizedValue(SOFTWARE_PRODUCTS_CATEGORY_NAME_KEY);
        String vendorAttributeName = getDefaultLocalizedValue(SOFTWARE_PRODUCTS_VENDOR_ATTRIBUTE_NAME_KEY);

        ensureCategoryWithAttribute(SOFTWARE_PRODUCTS_CATEGORY_ID,
                categoryName,
                CategoryItem.class.getSimpleName(),
                SOFTWARE_PRODUCTS_VENDOR_ATTR_ID,
                vendorAttributeName,
                VENDOR_ATTRIBUTE_CODE);

        findCategory(SOFTWARE_PRODUCTS_CATEGORY_ID, categoryName, CategoryItem.class.getSimpleName())
                .ifPresent(category -> ensureLocalizedCategoryName(category, SOFTWARE_PRODUCTS_CATEGORY_NAME_KEY));
        findAttribute(SOFTWARE_PRODUCTS_VENDOR_ATTR_ID, VENDOR_ATTRIBUTE_CODE)
                .ifPresent(attribute -> ensureLocalizedAttributeName(attribute, SOFTWARE_PRODUCTS_VENDOR_ATTRIBUTE_NAME_KEY));
    }

    private void createSalesTerritoryCategoryIfNeeded() {
        String categoryName = getDefaultLocalizedValue(SALES_TERRITORY_CATEGORY_NAME_KEY);
        String salesAreaName = getDefaultLocalizedValue(SALES_TERRITORY_SALES_AREA_NAME_KEY);

        ensureCategoryWithAttribute(SALES_TERRITORY_CATEGORY_ID,
                categoryName,
                Client.class.getSimpleName(),
                SALES_TERRITORY_SALES_AREA_ATTR_ID,
                salesAreaName,
                SALES_TERRITORY_SALES_AREA_CODE);

        findCategory(SALES_TERRITORY_CATEGORY_ID, categoryName, Client.class.getSimpleName())
                .ifPresent(category -> ensureLocalizedCategoryName(category, SALES_TERRITORY_CATEGORY_NAME_KEY));
        findAttribute(SALES_TERRITORY_SALES_AREA_ATTR_ID, SALES_TERRITORY_SALES_AREA_CODE)
                .ifPresent(attribute -> ensureLocalizedAttributeName(attribute, SALES_TERRITORY_SALES_AREA_NAME_KEY));
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

    private void ensureLocalizedCategoryName(Category category, String messageKey) {
        ensureLocalizedName(category::getName, category::getLocaleNames,
                category::setName, category::setLocaleNames,
                messageKey, category);
    }

    private void ensureLocalizedAttributeName(CategoryAttribute attribute, String messageKey) {
        ensureLocalizedName(attribute::getName, attribute::getLocaleNames,
                attribute::setName, attribute::setLocaleNames,
                messageKey, attribute);
    }

    private void ensureLocalizedName(Supplier<String> nameSupplier,
                                     Supplier<String> localeNamesSupplier,
                                     Consumer<String> nameSetter,
                                     Consumer<String> localeNamesSetter,
                                     String messageKey,
                                     Object entityToSave) {
        String localeNames = buildLocaleNames(messageKey);
        String defaultName = getDefaultLocalizedValue(messageKey);

        if (Objects.equals(nameSupplier.get(), defaultName)
                && Objects.equals(localeNamesSupplier.get(), localeNames)) {
            return;
        }

        nameSetter.accept(defaultName);
        localeNamesSetter.accept(localeNames);
        dataManager.save(entityToSave);
    }

    private String buildLocaleNames(String messageKey) {
        StringBuilder localeNames = new StringBuilder();

        for (Locale locale : getAvailableLocales()) {
            if (!localeNames.isEmpty()) {
                localeNames.append('\n');
            }

            localeNames.append(LocaleResolver.localeToString(locale))
                    .append('=')
                    .append(messages.getMessage(messageKey, locale));
        }

        return localeNames.toString();
    }

    private String getDefaultLocalizedValue(String messageKey) {
        return messages.getMessage(messageKey, getDefaultLocale());
    }

    private Locale getDefaultLocale() {
        return getAvailableLocales().stream()
                .findFirst()
                .orElse(Locale.ENGLISH);
    }

    private List<Locale> getAvailableLocales() {
        return coreProperties.getAvailableLocales();
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
