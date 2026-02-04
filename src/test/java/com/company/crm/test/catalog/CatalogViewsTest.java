package com.company.crm.test.catalog;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.security.ManagerRole;
import com.company.crm.security.SupervisorRole;
import com.company.crm.util.UniqueValues;
import com.company.crm.util.extenstion.AuthenticatedAs;
import com.company.crm.view.catalog.CategoryItemDetailView;
import com.company.crm.view.catalog.CategoryItemListView;
import com.company.crm.view.catalog.CategoryListView;
import com.company.crm.view.category.CategoryDetailView;
import io.jmix.flowui.view.DetailView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogViewsTest extends AbstractUiTest {

    @Test
    void opensCategoryListView() {
        var view = viewTestSupport.navigateTo(CategoryListView.class);
        assertThat(view).isInstanceOf(CategoryListView.class);
    }

    @Test
    void opensCategoryDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(Category.class, CategoryDetailView.class);
        assertThat(view).isInstanceOf(CategoryDetailView.class);
    }

    @Test
    @AuthenticatedAs(ManagerRole.CODE)
    void managerCannotSaveCategory() {
        viewTestSupport.navigateToNewEntityDetail(Category.class);
        viewTestSupport.<CategoryDetailView>withCurrentView(view ->
                fillCategoryDetailAndSave(view, true));
    }

    @Test
    @AuthenticatedAs(SupervisorRole.CODE)
    void supervisorCanSaveCategory() {
        viewTestSupport.navigateToNewEntityDetail(Category.class);
        viewTestSupport.<CategoryDetailView>withCurrentView(view ->
                fillCategoryDetailAndSave(view, false));
    }

    @Test
    void opensCategoryItemListView() {
        var view = viewTestSupport.navigateTo(CategoryItemListView.class);
        assertThat(view).isInstanceOf(CategoryItemListView.class);
    }

    @Test
    void opensCategoryItemDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(CategoryItem.class);
        assertThat(view).isInstanceOf(CategoryItemDetailView.class);
    }

    @Test
    @AuthenticatedAs(ManagerRole.CODE)
    void managerCannotSaveCategoryItem() {
        viewTestSupport.navigateToNewEntityDetail(CategoryItem.class);
        viewTestSupport.<CategoryItemDetailView>withCurrentView(view ->
                fillCategoryItemDetailAndSave(view, true));
    }

    @Test
    @AuthenticatedAs(SupervisorRole.CODE)
    void supervisorCanSaveCategoryItem() {
        viewTestSupport.navigateToNewEntityDetail(CategoryItem.class);
        viewTestSupport.<CategoryItemDetailView>withCurrentView(view ->
                fillCategoryItemDetailAndSave(view, false));
    }

    private void fillCategoryDetailAndSave(CategoryDetailView view, boolean assertSaveThrowsException) {
        viewTestSupport.setComponentValue("nameField", UniqueValues.string());
        viewTestSupport.setComponentValue("codeField", UniqueValues.string());
        saveDetailView(view, assertSaveThrowsException);
    }

    private void fillCategoryItemDetailAndSave(CategoryItemDetailView view, boolean assertSaveThrowsException) {
        viewTestSupport.setComponentValue("nameField", UniqueValues.string());
        viewTestSupport.setComponentValue("codeField", UniqueValues.string());
        viewTestSupport.setComponentValue("priceField", "10");
        viewTestSupport.setComponentValue("uomField", UomType.PIECES);
        saveDetailView(view, assertSaveThrowsException);
    }

    private static void saveDetailView(DetailView<?> view, boolean assertSaveThrowsException) {
        if (assertSaveThrowsException) {
            assertThrows(RuntimeException.class, view::save);
        } else {
            assertDoesNotThrow(view::save);
        }
    }
}
