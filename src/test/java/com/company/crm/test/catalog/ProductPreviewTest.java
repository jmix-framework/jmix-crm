package com.company.crm.test.catalog;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.util.UniqueValues;
import com.company.crm.view.catalog.CategoryItemListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.FileRef;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.sidepanellayout.SidePanelLayout;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.ViewControllerUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static com.company.crm.model.datatype.PriceDataType.defaultFormat;
import static org.assertj.core.api.Assertions.assertThat;

class ProductPreviewTest extends AbstractUiTest {

    @Test
    void productNameOpensPreviewWithDetailsAndImagePlaceholder() {
        CategoryItem item = entities.createAndSaveEntity(CategoryItem.class, product -> {
            product.setName("Product preview");
            product.setCode(UniqueValues.string());
            product.setPrice(new BigDecimal("125.50"));
            product.setUom(UomType.PIECES);
            product.setDescription("Plain <b>product description</b>");
        });
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);

        nameButton(grid(view), item).click();

        assertThat(preview(view).isSidePanelOpened()).isTrue();
        assertThat(previewItem(view).getId()).isEqualTo(item.getId());
        assertThat(text(view, "previewName")).isEqualTo(item.getName());
        assertThat(text(view, "previewCode")).isEqualTo(item.getCode());
        assertThat(text(view, "previewCategory")).isEmpty();
        assertThat(text(view, "previewPrice"))
                .isEqualTo(defaultFormat(item.getPrice(), applicationContext.getBean(DatatypeFormatter.class)));
        assertThat(text(view, "previewDescription")).isEqualTo(item.getDescription());
        JmixImage<?> image = viewTestSupport.getComponent(view, "previewImage");
        assertThat(image.getSrc()).isEqualTo("images/no_image.svg");
        assertThat(thumbnail(grid(view), item).getSrc()).isEqualTo("images/no_image.svg");
    }

    @Test
    void rowClicksUpdatePreviewAndKeepMultipleRowsSelected() {
        CategoryItem first = item("First product");
        CategoryItem second = item("Second product");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        DataGrid<CategoryItem> grid = grid(view);

        clickRow(grid, first, grid.getColumnByKey("uom"));
        clickRow(grid, second, grid.getColumnByKey("price"));

        assertThat(preview(view).isSidePanelOpened()).isTrue();
        assertThat(previewItem(view).getId()).isEqualTo(second.getId());
        assertThat(grid.getSelectedItems()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void clickingThumbnailAgainClosesPreviewAndDeselectsOnlyThatProduct() {
        CategoryItem first = item("First selected product");
        CategoryItem second = item("Toggle preview product");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        DataGrid<CategoryItem> grid = grid(view);

        clickRow(grid, first, grid.getColumnByKey("image"));
        clickRow(grid, second, grid.getColumnByKey("image"));
        clickRow(grid, second, grid.getColumnByKey("image"));

        assertThat(preview(view).isSidePanelOpened()).isFalse();
        assertThat(grid.getSelectedItems()).containsExactly(first);

        clickRow(grid, second, grid.getColumnByKey("image"));

        assertThat(preview(view).isSidePanelOpened()).isTrue();
        assertThat(previewItem(view).getId()).isEqualTo(second.getId());
        assertThat(grid.getSelectedItems()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void clickingProductNameAgainClosesPreviewAndDeselectsProduct() {
        CategoryItem item = item("Toggle name product");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        DataGrid<CategoryItem> grid = grid(view);

        nameButton(grid, item).click();
        assertThat(grid.getSelectedItems()).containsExactly(item);

        nameButton(grid, item).click();

        assertThat(preview(view).isSidePanelOpened()).isFalse();
        assertThat(grid.getSelectedItems()).isEmpty();
    }

    @Test
    void thumbnailReopensClosedPanelWithoutDeselectingProduct() {
        CategoryItem item = item("Reopen preview product");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        DataGrid<CategoryItem> grid = grid(view);
        clickRow(grid, item, grid.getColumnByKey("image"));
        preview(view).closeSidePanel();

        clickRow(grid, item, grid.getColumnByKey("image"));

        assertThat(preview(view).isSidePanelOpened()).isTrue();
        assertThat(grid.getSelectedItems()).containsExactly(item);
    }

    @Test
    void checkboxSelectionDoesNotOpenPreview() {
        CategoryItem item = item("Checkbox product");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        DataGrid<CategoryItem> grid = grid(view);

        grid.select(item);
        clickRow(grid, item, null);

        assertThat(grid.getSelectedItems()).containsExactly(item);
        assertThat(preview(view).isSidePanelOpened()).isFalse();
    }

    @Test
    void lookupReturnsSelectedProductWithoutOpeningPreview() {
        CategoryItem item = item("Lookup product");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        var selected = new ArrayList<CategoryItem>();
        view.setSelectionHandler(selected::addAll);
        view.setLookupComponentMultiSelect(false);
        DataGrid<CategoryItem> grid = grid(view);

        clickRow(grid, item, grid.getColumnByKey("uom"));
        clickRow(grid, item, grid.getColumnByKey("image"));
        clickRow(grid, item, grid.getColumnByKey("image"));

        assertThat(preview(view).isSidePanelOpened()).isFalse();
        assertThat(grid.getSelectedItems()).containsExactly(item);
        viewTestSupport.click(view, "selectButton");
        assertThat(selected).containsExactly(item);
    }

    @Test
    void productNameSelectsLookupItemWithoutOpeningPreview() {
        CategoryItem item = item("Lookup name product");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        view.setSelectionHandler(items -> { });
        view.setLookupComponentMultiSelect(false);

        nameButton(grid(view), item).click();
        nameButton(grid(view), item).click();

        assertThat(grid(view).getSelectedItems()).containsExactly(item);
        assertThat(preview(view).isSidePanelOpened()).isFalse();
    }

    @Test
    void filteringClosesPreviewAndLoadsMatchingProducts() {
        CategoryItem first = item("Preview laptop");
        CategoryItem second = item("Matching printer");
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);
        nameButton(grid(view), first).click();

        viewTestSupport.setComponentValue("items_searchField", "Matching printer");

        CollectionContainer<CategoryItem> items = ViewControllerUtils.getViewData(view).getContainer("categoryItemsDc");
        assertThat(items.getItems()).containsExactly(second);
        assertThat(preview(view).isSidePanelOpened()).isFalse();
    }

    @Test
    void missingImageFileUsesPlaceholder() {
        CategoryItem item = entities.createAndSaveEntity(CategoryItem.class, product -> {
            product.setName("Missing product image");
            product.setCode(UniqueValues.string());
            product.setPrice(BigDecimal.TEN);
            product.setUom(UomType.PIECES);
            product.setImage(new FileRef("crm", "2026/01/01/" + UniqueValues.string() + ".png", "missing.png"));
        });
        CategoryItemListView view = viewTestSupport.navigateTo(CategoryItemListView.class);

        nameButton(grid(view), item).click();

        JmixImage<?> image = viewTestSupport.getComponent(view, "previewImage");
        assertThat(image.getSrc()).isEqualTo("images/no_image.svg");
        assertThat(thumbnail(grid(view), item).getSrc()).isEqualTo("images/no_image.svg");
    }

    private CategoryItem item(String name) {
        return entities.categoryItem(name, UniqueValues.string(), null, BigDecimal.TEN, UomType.PIECES);
    }

    private DataGrid<CategoryItem> grid(CategoryItemListView view) {
        return viewTestSupport.getComponent(view, "categoryItemsDataGrid");
    }

    private SidePanelLayout preview(CategoryItemListView view) {
        return viewTestSupport.getComponent(view, "productPreview");
    }

    private CategoryItem previewItem(CategoryItemListView view) {
        InstanceContainer<CategoryItem> item = ViewControllerUtils.getViewData(view).getContainer("productPreviewDc");
        return item.getItem();
    }

    private String text(CategoryItemListView view, String id) {
        return ((HasText) viewTestSupport.getComponent(view, id)).getText();
    }

    private Button nameButton(DataGrid<CategoryItem> grid, CategoryItem item) {
        return (Button) render(grid, "name", item);
    }

    private JmixImage<?> thumbnail(DataGrid<CategoryItem> grid, CategoryItem item) {
        return (JmixImage<?>) render(grid, "image", item);
    }

    private Component render(DataGrid<CategoryItem> grid, String columnKey, CategoryItem item) {
        Grid.Column<CategoryItem> column = grid.getColumnByKey(columnKey);
        assertThat(column).as("Column %s", columnKey).isNotNull();
        return ((ComponentRenderer<? extends Component, CategoryItem>) column.getRenderer()).createComponent(item);
    }

    private void clickRow(DataGrid<CategoryItem> grid, CategoryItem item, Grid.Column<CategoryItem> column) {
        String itemKey = grid.getDataCommunicator().getKeyMapper().key(item);
        String columnId = column == null ? "" : column.getElement().getProperty("_flowId");
        ComponentUtil.fireEvent(grid, new ItemClickEvent<>(grid, true, itemKey, columnId,
                0, 0, 0, 0, 1, 0, false, false, false, false));
    }
}
