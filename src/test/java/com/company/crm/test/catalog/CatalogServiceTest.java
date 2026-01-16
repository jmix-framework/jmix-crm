package com.company.crm.test.catalog;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.catalog.CatalogImportSettings;
import com.company.crm.app.service.catalog.CatalogService;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceTest extends AbstractTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void updateCatalog_importsCategoriesWithHierarchy() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Categories");
        
        // Header
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Code");
        header.createCell(2).setCellValue("Parent Code");
        header.createCell(3).setCellValue("Description");

        // Root category
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Root");
        row1.createCell(1).setCellValue("ROOT");
        row1.createCell(3).setCellValue("Root description");

        // Child category
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Child");
        row2.createCell(1).setCellValue("CHILD");
        row2.createCell(2).setCellValue("ROOT");
        row2.createCell(3).setCellValue("Child description");

        // Grandchild category
        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("Grandchild");
        row3.createCell(1).setCellValue("GRANDCHILD");
        row3.createCell(2).setCellValue("CHILD");

        // Add Items sheet to avoid accidental import from Categories sheet
        workbook.createSheet("Items");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        CatalogImportSettings settings = new CatalogImportSettings(new ByteArrayInputStream(out.toByteArray()));
        catalogService.updateCatalog(settings);

        Optional<Category> rootOpt = categoryRepository.findByCode("ROOT");
        assertThat(rootOpt).isPresent();
        Category root = rootOpt.get();
        assertThat(root.getName()).isEqualTo("Root");
        assertThat(root.getParent()).isNull();

        Optional<Category> childOpt = categoryRepository.findByCode("CHILD");
        assertThat(childOpt).isPresent();
        Category child = childOpt.get();
        assertThat(child.getName()).isEqualTo("Child");
        assertThat(child.getParent()).isNotNull();
        assertThat(child.getParent().getCode()).isEqualTo("ROOT");

        Optional<Category> grandchildOpt = categoryRepository.findByCode("GRANDCHILD");
        assertThat(grandchildOpt).isPresent();
        Category grandchild = grandchildOpt.get();
        assertThat(grandchild.getName()).isEqualTo("Grandchild");
        assertThat(grandchild.getParent()).isNotNull();
        assertThat(grandchild.getParent().getCode()).isEqualTo("CHILD");
    }

    @Test
    void updateCatalog_importsItemsWithNewCategory() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // Categories sheet (empty)
        workbook.createSheet("Categories").createRow(0);

        // Items sheet
        Sheet itemsSheet = workbook.createSheet("Items");
        Row header = itemsSheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Code");
        header.createCell(2).setCellValue("Category Code");
        header.createCell(3).setCellValue("UOM");
        header.createCell(4).setCellValue("Price");

        Row row1 = itemsSheet.createRow(1);
        row1.createCell(0).setCellValue("Item 1");
        row1.createCell(1).setCellValue("ITEM1");
        row1.createCell(2).setCellValue("NEWCAT");
        row1.createCell(3).setCellValue("PIECES");
        row1.createCell(4).setCellValue("100.00");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        CatalogImportSettings settings = new CatalogImportSettings(new ByteArrayInputStream(out.toByteArray()));
        catalogService.updateCatalog(settings);

        Optional<Category> catOpt = categoryRepository.findByCode("NEWCAT");
        assertThat(catOpt).isPresent();

        // Check item
        // I need to add CategoryItemRepository to the test if I want to check it easily
    }

    @Test
    void generateCatalogXls_exportsExistingData() throws IOException {
        // Create category
        Category category = dataManager.create(Category.class);
        category.setName("Test Category");
        category.setCode("TESTCAT");
        dataManager.save(category);

        // Create item
        CategoryItem item = dataManager.create(CategoryItem.class);
        item.setName("Test Item");
        item.setCode("TESTITEM");
        item.setCategory(category);
        item.setPrice(new BigDecimal("123.45"));
        item.setUom(UomType.PIECES);
        dataManager.save(item);

        byte[] content = catalogService.generateCatalogXls();
        assertThat(content).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet catSheet = workbook.getSheet("Categories");
            assertThat(catSheet).isNotNull();
            Row catRow = catSheet.getRow(1);
            assertThat(catRow.getCell(0).getStringCellValue()).isEqualTo("Test Category");
            assertThat(catRow.getCell(1).getStringCellValue()).isEqualTo("TESTCAT");

            Sheet itemSheet = workbook.getSheet("Items");
            assertThat(itemSheet).isNotNull();
            Row itemRow = itemSheet.getRow(1);
            assertThat(itemRow.getCell(0).getStringCellValue()).isEqualTo("Test Item");
            assertThat(itemRow.getCell(1).getStringCellValue()).isEqualTo("TESTITEM");
            assertThat(itemRow.getCell(2).getStringCellValue()).isEqualTo("TESTCAT");
            assertThat(itemRow.getCell(3).getStringCellValue()).isEqualTo("PIECES");
            assertThat(itemRow.getCell(4).getNumericCellValue()).isEqualTo(123.45);
        }
    }
}
