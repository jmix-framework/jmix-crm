package com.company.crm.app.service.catalog;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import com.company.crm.model.catalog.item.UomType;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FluentValuesLoader;
import io.jmix.core.UnconstrainedDataManager;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final CategoryItemRepository categoryItemRepository;
    private final UnconstrainedDataManager dataManager;
    private final FileStorage fileStorage;

    public CatalogService(CategoryItemRepository categoryItemRepository,
                          UnconstrainedDataManager dataManager,
                          FileStorage fileStorage) {
        this.categoryItemRepository = categoryItemRepository;
        this.dataManager = dataManager;
        this.fileStorage = fileStorage;
    }

    public Map<Category, List<CategoryItem>> importCatalog(InputStream xlsxInputStream,
                                                           @Nullable Function<String, InputStream> imageDataProvider) {
        try (Workbook workbook = new XSSFWorkbook(xlsxInputStream)) {
            Map<String, Category> categoriesByCode = importCategories(workbook.getSheet("Categories"));
            return importItems(workbook.getSheet("Items"), categoriesByCode, imageDataProvider);
        } catch (IOException e) {
            throw new RuntimeException("Failed to import catalog from XLSX", e);
        }
    }

    //@formatter:off
    /**
     * Retrieves the top-ordered items based on their quantity,
     * optionally limited to a specific number of items.
     *
     * @param limit         an optional integer specifying the maximum number of items to return.
     *                      If null or not greater than 0, no limit is applied.
     *
     * @param dateRange     an optional parameter specifying the date range for the query.
     *
     * @return              a map where the keys are the top {@link CategoryItem} objects
     *                      and the values are their respective total quantities as {@code BigDecimal}.
     */
    //@formatter:on
    public Map<CategoryItem, BigDecimal> getBestOrderedItems(@Nullable Integer limit,
                                                             @Nullable LocalDateRange dateRange) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder
                .append("select e.categoryItem as categoryItem, sum(e.quantity) as quantity ")
                .append("from OrderItem e ");

        if (dateRange != null) {
            queryBuilder.append("where e.order.date >= :startDate and e.order.date <= :endDate ");
        }

        queryBuilder.append("group by e.categoryItem ")
                .append("order by quantity desc");

        FluentValuesLoader loader = categoryItemRepository
                .fluentValuesLoader(queryBuilder.toString())
                .properties("categoryItem", "quantity")
                .maxResults(limit != null ? limit : 0);

        if (dateRange != null) {
            loader.parameter("startDate", dateRange.startDate())
                    .parameter("endDate", dateRange.endDate());
        }

        return loader.list().stream().collect(Collectors.toMap(
                keyValue -> keyValue.getValue("categoryItem"),
                keyValue -> keyValue.getValue("quantity")
        ));
    }

    private Map<String, Category> importCategories(Sheet sheet) {
        Map<String, Category> categoriesByCode = new HashMap<>();
        if (sheet == null) return categoriesByCode;

        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = getCellValueAsString(row, 0, dataFormatter);
            String code = getCellValueAsString(row, 1, dataFormatter);
            String parentCode = getCellValueAsString(row, 2, dataFormatter);
            String description = getCellValueAsString(row, 3, dataFormatter);

            if (name == null || code == null) continue;

            Category category = dataManager.create(Category.class);
            category.setName(name);
            category.setCode(code);
            category.setDescription(description);

            if (parentCode != null) {
                category.setParent(categoriesByCode.get(parentCode));
            }

            category = dataManager.save(category);
            categoriesByCode.put(code, category);
        }
        return categoriesByCode;
    }

    private Map<Category, List<CategoryItem>> importItems(Sheet sheet,
                                                          Map<String, Category> categoriesByCode,
                                                          @Nullable Function<String, InputStream> imageDataProvider) {
        Map<Category, List<CategoryItem>> result = new HashMap<>();
        if (sheet == null) return result;

        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = getCellValueAsString(row, 0, dataFormatter);
            String code = getCellValueAsString(row, 1, dataFormatter);
            String categoryCode = getCellValueAsString(row, 2, dataFormatter);
            String uomStr = getCellValueAsString(row, 3, dataFormatter);
            String priceStr = getCellValueAsString(row, 4, dataFormatter);
            String description = getCellValueAsString(row, 5, dataFormatter);
            String imageName = getCellValueAsString(row, 6, dataFormatter);

            if (name == null || code == null || categoryCode == null) continue;

            Category category = categoriesByCode.get(categoryCode);
            if (category == null) {
                log.warn("Category with code {} not found for item {}", categoryCode, name);
                continue;
            }

            CategoryItem item = dataManager.create(CategoryItem.class);
            item.setName(name);
            item.setCode(code);
            item.setCategory(category);
            item.setDescription(description);
            if (uomStr != null) {
                try {
                    item.setUom(UomType.valueOf(uomStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UOM {} for item {}", uomStr, name);
                }
            }
            if (priceStr != null) {
                try {
                    item.setPrice(new BigDecimal(priceStr.replace(",", ".")));
                } catch (NumberFormatException e) {
                    log.warn("Invalid price {} for item {}", priceStr, name);
                }
            }

            if (imageName != null && imageDataProvider != null) {
                InputStream imageStream = imageDataProvider.apply(imageName);
                if (imageStream != null) {
                    FileRef fileRef = fileStorage.saveStream(imageName, imageStream);
                    item.setImage(fileRef);
                }
            }

            item = dataManager.save(item);
            result.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    @Nullable
    private String getCellValueAsString(Row row, int cellIndex, DataFormatter dataFormatter) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        String value = dataFormatter.formatCellValue(cell);
        return value.isBlank() ? null : value;
    }
}
