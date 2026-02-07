package com.company.crm.app.service.catalog;

import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

import static com.company.crm.app.service.catalog.CatalogService.DEFAULT_IMAGE_DATA_PROVIDER;

public record CatalogImportSettings(
        InputStream xlsxInputStream,
        CategoryMappingInfo categoryMappingInfo,
        CategoryItemMappingInfo categoryItemMappingInfo
) {

    public CatalogImportSettings(InputStream xlsxInputStream) {
        this(xlsxInputStream, CategoryMappingInfo.DEFAULT, CategoryItemMappingInfo.DEFAULT);
    }

    public interface MappingInfo<T extends UuidEntity> {
        @Nullable
        String sheetName();

        Class<T> entityClass();

        Map<String, Integer> cell2FieldMappings();

        @Nullable
        default Function<String, InputStream> imageDataProvider() {
            return DEFAULT_IMAGE_DATA_PROVIDER;
        }
    }

    public abstract static class AbstractMappingInfo<T extends UuidEntity> implements MappingInfo<T> {

        protected final String sheetName;
        protected final Map<String, Integer> cell2FieldMappings;

        public AbstractMappingInfo(String sheetName, Map<String, Integer> cell2FieldMappings) {
            this.sheetName = sheetName;
            this.cell2FieldMappings = cell2FieldMappings;
        }

        @Nullable
        @Override
        public String sheetName() {
            return sheetName;
        }

        @Override
        public Map<String, Integer> cell2FieldMappings() {
            return cell2FieldMappings;
        }
    }

    public static class CategoryMappingInfo extends AbstractMappingInfo<Category> {

        public static final String DEFAULT_SHEET_NAME = "Categories";
        public static final Map<String, Integer> DEFAULT_CELL_MAPPINGS = Map.of(
                "name", 0,
                "code", 1,
                "parentCode", 2,
                "description", 3
        );

        public static final CategoryMappingInfo DEFAULT = new CategoryMappingInfo();

        public CategoryMappingInfo() {
            this(DEFAULT_SHEET_NAME, DEFAULT_CELL_MAPPINGS);
        }

        public CategoryMappingInfo(String sheetName, Map<String, Integer> cell2FieldMappings) {
            super(sheetName, cell2FieldMappings);
        }

        @Override
        public Class<Category> entityClass() {
            return Category.class;
        }
    }

    public static class CategoryItemMappingInfo extends AbstractMappingInfo<CategoryItem> {

        public static final String DEFAULT_SHEET_NAME = "Items";
        public static final Map<String, Integer> DEFAULT_CELL_MAPPINGS = Map.of(
                "name", 0,
                "code", 1,
                "categoryCode", 2,
                "uom", 3,
                "price", 4,
                "description", 5,
                "imageName", 6
        );

        public static final CategoryItemMappingInfo DEFAULT = new CategoryItemMappingInfo();

        public CategoryItemMappingInfo() {
            this(DEFAULT_SHEET_NAME, DEFAULT_CELL_MAPPINGS);
        }

        public CategoryItemMappingInfo(String sheetName, Map<String, Integer> cell2FieldMappings) {
            super(sheetName, cell2FieldMappings);
        }

        @Override
        public Class<CategoryItem> entityClass() {
            return CategoryItem.class;
        }
    }
}
