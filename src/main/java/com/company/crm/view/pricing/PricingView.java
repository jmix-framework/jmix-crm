package com.company.crm.view.pricing;

import com.company.crm.app.service.catalog.CatalogImportSettings;
import com.company.crm.app.service.catalog.CatalogService;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.settings.CrmSettings;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.appsettings.AppSettings;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

@Route(value = "pricing", layout = MainView.class)
@ViewController(id = "PricingView")
@ViewDescriptor(path = "pricing-view.xml")
public class PricingView extends StandardView {

    @Autowired
    private Dialogs dialogs;
    @Autowired
    private AppSettings appSettings;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private CatalogService catalogService;

    @ViewComponent
    private Span currentVatValue;
    @ViewComponent
    private CollectionLoader<CategoryItem> categoryItemsDl;

    @Subscribe
    private void onInit(final InitEvent event) {
        updateCurrentVatSpanValue();
    }

    @Supply(to = "categoryItemsDataGrid.name", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridNameRenderer() {
        return crmRenderers.entityLink(Function.identity());
    }

    @Supply(to = "categoryItemsDataGrid.code", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridCodeRenderer() {
        return crmRenderers.categoryItemCode();
    }

    @Supply(to = "categoryItemsDataGrid.[category.code]", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridCategoryCodeRenderer() {
        return crmRenderers.categoryItemCategoryCode();
    }

    @Subscribe("categoryItemsDataGrid.downloadXls")
    private void onCategoryItemsDataGridDownloadXls(final ActionPerformedEvent event) {
        catalogService.downloadCatalogXls();
        categoryItemsDl.load();
    }

    @Subscribe("categoryItemsDataGrid.updateFromXls")
    private void onCategoryItemsDataGridUpdateFromXls(final ActionPerformedEvent event) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        FileUploadField xlsFileUploader = uiComponents.create(FileUploadField.class);
        xlsFileUploader.setAcceptedFileTypes(".xls", ".xlsx");
        xlsFileUploader.addFileUploadSucceededListener(e -> {
            byte[] fileContent = e.getSource().getValue();
            if (fileContent != null) {
                catalogService.updateCatalog(new CatalogImportSettings(new ByteArrayInputStream(fileContent)));
                dialog.close();
            }
        });

        dialog.add(xlsFileUploader);
        dialog.open();
    }

    @Subscribe("categoryItemsDataGrid.changeVat")
    private void onCategoryItemsDataGridChangeVat(final ActionPerformedEvent event) {
        CrmSettings crmSettings = loadCrmSettings();
        dialogs.createInputDialog(this)
                .withParameters(
                        InputParameter.bigDecimalParameter("vat")
                                .withLabel("VAT, %")
                                .withDefaultValue(crmSettings.getDefaultVatPercent()))
                .withActions(
                        DialogActions.OK_CANCEL,
                        e -> {
                            if (e.closedWith(DialogOutcome.OK)) {
                                crmSettings.setDefaultVatPercent(e.getValue("vat"));
                                appSettings.save(crmSettings);
                                updateCurrentVatSpanValue();
                            }
                        })
                .open();
    }

    private void updateCurrentVatSpanValue() {
        currentVatValue.setText("VAT = " + loadCrmSettings().getDefaultVatPercent() + "%");
    }

    private CrmSettings loadCrmSettings() {
        return appSettings.load(CrmSettings.class);
    }
}