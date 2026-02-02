package com.company.crm.app.service.catalog;

import com.company.crm.app.service.catalog.CatalogImportSettings.CategoryItemMappingInfo;
import com.company.crm.app.service.catalog.CatalogImportSettings.CategoryMappingInfo;
import com.company.crm.app.service.catalog.CatalogImportSettings.MappingInfo;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import com.company.crm.model.catalog.item.UomType;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FluentValuesLoader;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.download.Downloader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final FileStorage fileStorage;
    private final UnconstrainedDataManager dataManager;
    private final CategoryRepository categoryRepository;
    private final CategoryItemRepository categoryItemRepository;
    private final ObjectProvider<Downloader> downloaderProvider;

    public CatalogService(CategoryItemRepository categoryItemRepository, UnconstrainedDataManager dataManager,
                          FileStorage fileStorage, CategoryRepository categoryRepository, ObjectProvider<Downloader> downloaderProvider, UiComponents uiComponents) {
        this.categoryItemRepository = categoryItemRepository;
        this.dataManager = dataManager;
        this.fileStorage = fileStorage;
        this.categoryRepository = categoryRepository;
        this.downloaderProvider = downloaderProvider;
    }

    public void downloadCatalogXls() {
        try {
            byte[] content = generateCatalogXls();
            downloaderProvider.getObject().download(content, "catalog.xlsx");
        } catch (Throwable e) {
            log.error("Error when downloading catalog in XLS format", e);
        }
    }

    public byte[] generateCatalogXls() {
        try (XSSFWorkbook workbook = createWorkbook()) {
            fillCategoriesSheet(workbook);
            fillItemsSheet(workbook);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error when generating catalog in XLS format", e);
        }
    }

    private void fillCategoriesSheet(XSSFWorkbook workbook) {
        var sheet = workbook.createSheet(CategoryMappingInfo.DEFAULT_SHEET_NAME);
        Row headerRow = sheet.createRow(0);
        CategoryMappingInfo.DEFAULT_CELL_MAPPINGS.forEach((field, index) ->
                headerRow.createCell(index).setCellValue(field));

        List<Category> categories = categoryRepository.findAll();
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(CategoryMappingInfo.DEFAULT_CELL_MAPPINGS.get("name")).setCellValue(category.getName());
            row.createCell(CategoryMappingInfo.DEFAULT_CELL_MAPPINGS.get("code")).setCellValue(category.getCode());
            if (category.getParent() != null) {
                row.createCell(CategoryMappingInfo.DEFAULT_CELL_MAPPINGS.get("parentCode")).setCellValue(category.getParent().getCode());
            }
            row.createCell(CategoryMappingInfo.DEFAULT_CELL_MAPPINGS.get("description")).setCellValue(category.getDescription());
        }
    }

    private void fillItemsSheet(XSSFWorkbook workbook) {
        var sheet = workbook.createSheet(CategoryItemMappingInfo.DEFAULT_SHEET_NAME);
        Row headerRow = sheet.createRow(0);
        CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.forEach((field, index) ->
                headerRow.createCell(index).setCellValue(field));

        List<CategoryItem> items = categoryItemRepository.findAll();
        for (int i = 0; i < items.size(); i++) {
            CategoryItem item = items.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.get("name")).setCellValue(item.getName());
            row.createCell(CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.get("code")).setCellValue(item.getCode());
            if (item.getCategory() != null) {
                row.createCell(CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.get("categoryCode")).setCellValue(item.getCategory().getCode());
            }
            if (item.getUom() != null) {
                row.createCell(CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.get("uom")).setCellValue(item.getUom().getId());
            }
            if (item.getPrice() != null) {
                row.createCell(CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.get("price")).setCellValue(item.getPrice().doubleValue());
            }
            row.createCell(CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.get("description")).setCellValue(item.getDescription());
            if (item.getImage() != null) {
                row.createCell(CategoryItemMappingInfo.DEFAULT_CELL_MAPPINGS.get("imageName")).setCellValue(item.getImage().getFileName());
            }
        }
    }

    private XSSFWorkbook createWorkbook() {
        var workbook = new XSSFWorkbook();
        var headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return workbook;
    }

    public Map<Category, List<CategoryItem>> updateCatalog(CatalogImportSettings importSettings) {
        try (Workbook workbook = new XSSFWorkbook(importSettings.xlsxInputStream())) {
            Map<String, Category> categoriesByCode = importCategories(workbook, importSettings.categoryMappingInfo());
            return importItems(workbook, categoriesByCode, importSettings.categoryItemMappingInfo());
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
                keyValue -> keyValue.getValue("quantity"),
                (v1, v2) -> v1,
                java.util.LinkedHashMap::new)
        );
    }

    private Map<String, Category> importCategories(Workbook workbook, CategoryMappingInfo mappingInfo) {
        Map<String, Category> categoriesByCode = new HashMap<>();
        if (mappingInfo == null) {
            return categoriesByCode;
        }

        Sheet sheet = getSheet(workbook, mappingInfo);
        if (sheet == null) {
            return categoriesByCode;
        }

        DataFormatter dataFormatter = new DataFormatter();

        Map<String, String> parentCodesByCode = new HashMap<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            String name = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("name"), dataFormatter);
            String code = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("code"), dataFormatter);
            String parentCode = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("parentCode"), dataFormatter);
            String description = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("description"), dataFormatter);

            if (name == null || code == null) {
                continue;
            }

            Category category = getOrCreateCategory(categoriesByCode, code);
            category.setName(name);
            category.setCode(code);
            category.setDescription(description);

            if (parentCode != null) {
                parentCodesByCode.put(code, parentCode);
            }

            categoriesByCode.put(code, category);
        }

        for (Map.Entry<String, String> entry : parentCodesByCode.entrySet()) {
            String code = entry.getKey();
            String parentCode = entry.getValue();

            Category category = categoriesByCode.get(code);
            Category parent = getOrCreateCategory(categoriesByCode, parentCode);
            category.setParent(parent);

            categoriesByCode.put(parentCode, parent);
        }

        dataManager.save(categoriesByCode.values().toArray());

        return categoriesByCode;
    }

    private Map<Category, List<CategoryItem>> importItems(Workbook workbook,
                                                          Map<String, Category> categoriesByCode,
                                                          CategoryItemMappingInfo mappingInfo) {
        var itemsByCode = new HashMap<String, CategoryItem>();
        var result = new HashMap<Category, List<CategoryItem>>();

        Sheet sheet = getSheet(workbook, mappingInfo);
        if (sheet == null) {
            return result;
        }

        var dataFormatter = new DataFormatter();
        var imageDataProvider = mappingInfo.imageDataProvider();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            String name = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("name"), dataFormatter);
            String code = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("code"), dataFormatter);
            String categoryCode = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("categoryCode"), dataFormatter);
            String uomStr = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("uom"), dataFormatter);
            String priceStr = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("price"), dataFormatter);
            String description = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("description"), dataFormatter);
            String imageName = getCellValueAsString(row, mappingInfo.cell2FieldMappings().get("imageName"), dataFormatter);

            if (isAnyBlank(name, code, categoryCode)) {
                continue;
            }

            Category category = getOrCreateCategory(categoriesByCode, categoryCode);
            if (category == null) {
                log.warn("Category with code {} not found for item {}", categoryCode, name);
                continue;
            }

            CategoryItem item = getOrCreateItem(itemsByCode, code);
            item.setName(name);
            item.setCode(code);
            item.setCategory(category);
            item.setDescription(description);

            if (isNotBlank(uomStr)) {
                try {
                    item.setUom(UomType.valueOf(uomStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UOM {} for item {}", uomStr, name);
                }
            }

            if (isNotBlank(priceStr)) {
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

            itemsByCode.put(code, item);
            result.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
        }

        dataManager.save(categoriesByCode.values().toArray());
        dataManager.save(itemsByCode.values().toArray());

        return result;
    }

    private static Sheet getSheet(Workbook workbook, MappingInfo<?> mappingInfo) {
        Sheet sheet;
        String sheetName = mappingInfo.sheetName();
        if (StringUtils.isBlank(sheetName)) {
            sheet = workbook.getSheetAt(0);
        } else {
            sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
        }
        return sheet;
    }

    private Category createCategory(String code) {
        var category = dataManager.create(Category.class);
        category.setCode(code);
        category.setName(code);
        return category;
    }

    private Category getOrCreateCategory(String code) {
        return categoryRepository.findByCode(code)
                .orElseGet(() -> createCategory(code));
    }

    private Category getOrCreateCategory(Map<String, Category> categoriesByCode, String code) {
        Category category = categoriesByCode.get(code);
        if (category == null) {
            category = getOrCreateCategory(code);
            categoriesByCode.put(code, category);
        }
        return category;
    }

    private CategoryItem createItem(String code) {
        var item = dataManager.create(CategoryItem.class);
        item.setCode(code);
        return item;
    }

    private CategoryItem getOrCreateItem(String code) {
        return categoryItemRepository.findByCode(code)
                .orElseGet(() -> createItem(code));
    }

    private CategoryItem getOrCreateItem(Map<String, CategoryItem> itemsByCode, String code) {
        return Optional.ofNullable(itemsByCode.get(code))
                .orElseGet(() -> getOrCreateItem(code));
    }

    @Nullable
    private String getCellValueAsString(Row row, int cellIndex, DataFormatter dataFormatter) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }

        String value = dataFormatter.formatCellValue(cell);
        return value.isBlank() ? null : value;
    }

    static final Function<String, InputStream> DEFAULT_IMAGE_DATA_PROVIDER = fileName -> {
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            try {
                return new ByteArrayInputStream(IOUtils.toByteArray(new URI(fileName).toURL().openConnection()));
            } catch (Throwable ignored) {
                return null;
            }
        } else {
            return CatalogService.class.getResourceAsStream("/demo-data/images/" + fileName);
        }
    };
}
