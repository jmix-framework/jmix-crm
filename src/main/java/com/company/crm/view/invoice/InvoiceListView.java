package com.company.crm.view.invoice;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceRepository;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.PrimaryListView;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.company.crm.app.util.ui.datacontext.DataContextUtils.wrapCondition;
import static io.jmix.core.querycondition.PropertyCondition.equal;
import static io.jmix.core.querycondition.PropertyCondition.greaterOrEqual;
import static io.jmix.core.querycondition.PropertyCondition.lessOrEqual;
import static io.jmix.flowui.component.UiComponentUtils.getCurrentView;

@Route(value = "invoices", layout = MainView.class)
@ViewDescriptor("invoice-list-view.xml")
@ViewController(CrmConstants.ViewIds.INVOICE_LIST)
@LookupComponent("invoicesDataGrid")
@PrimaryListView(Invoice.class)
@DialogMode(width = "90%", resizable = true)
public class InvoiceListView extends StandardListView<Invoice> {

    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private InvoiceRepository invoiceRepository;

    @ViewComponent
    private CollectionContainer<Order> ordersDc;
    @ViewComponent
    private CollectionLoader<Order> ordersDl;
    @ViewComponent
    private CollectionContainer<Client> clientsDc;
    @ViewComponent
    private CollectionLoader<Client> clientsDl;

    @ViewComponent
    private EntityComboBox<Client> invoices_ClientComboBox;
    @ViewComponent
    private EntityComboBox<Order> invoices_OrderComboBox;
    @ViewComponent
    private JmixSelect<InvoiceStatus> invoices_StatusSelect;
    @ViewComponent
    private TypedDatePicker<LocalDate> invoices_FromDatePicker;
    @ViewComponent
    private TypedDatePicker<LocalDate> invoices_ToDatePicker;
    @ViewComponent
    private CollectionLoader<Invoice> invoicesDl;
    @ViewComponent
    private DataGrid<Invoice> invoicesDataGrid;

    private final LogicalCondition filtersCondition = LogicalCondition.and();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initialize();
    }

    @Install(to = "invoicesDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Invoice> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return invoiceRepository.findAll(pageable, wrapCondition(context, filtersCondition)).getContent();
    }

    @Install(to = "invoices_pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return invoiceRepository.count(wrapCondition(context, filtersCondition));
    }

    @Install(to = "invoicesDataGrid.removeAction", subject = "delegate")
    private void invoicesDataGridRemoveDelegate(final Collection<Invoice> collection) {
        invoiceRepository.deleteAll(collection);
    }

    @Supply(to = "invoicesDataGrid.status", subject = "renderer")
    private Renderer<Invoice> invoicesDataGridStatusRenderer() {
        return crmRenderers.invoiceStatus();
    }

    @Supply(to = "invoicesDataGrid.[order.number]", subject = "renderer")
    private Renderer<Invoice> invoicesDataGridOrderNumberRenderer() {
        return crmRenderers.invoiceOrderLink();
    }

    @Supply(to = "invoicesDataGrid.number", subject = "renderer")
    private Renderer<Invoice> invoicesDataGridNumberRenderer() {
        return crmRenderers.uniqueNumber(Invoice::getNumber);
    }

    @Supply(to = "invoicesDataGrid.client", subject = "renderer")
    private Renderer<Invoice> invoicesDataGridClientRenderer() {
        return crmRenderers.invoiceClientLink();
    }

    @Subscribe("invoicesDataGrid.downloadAction")
    private void onInvoicesDataGridDownloadAction(final ActionPerformedEvent event) {
        // TODO:
    }

    @Subscribe("invoicesDataGrid.emailAction")
    private void onInvoicesDataGridEmailAction(final ActionPerformedEvent event) {
        // TODO:
    }

    private void initialize() {
        loadData();
        registerUrlQueryParametersBinders();
        applyFilters();
        invoicesDataGrid.setItemDetailsRenderer(crmRenderers.invoiceDetails());
    }

    private void loadData() {
        clientsDl.load();
        ordersDl.load();
    }

    private void applyFilters() {
        updateFiltersCondition();
        invoicesDl.load();
    }

    private void updateFiltersCondition() {
        filtersCondition.getConditions().clear();
        addSearchConditions();
    }

    private void addSearchConditions() {
        addSearchByOrderCondition();
        addSearchByClientCondition();
        addSearchBySelectedStatus();
        addDateRangeConditions();
    }

    private void addSearchBySelectedStatus() {
        invoices_StatusSelect.getOptionalValue().ifPresent(status ->
                filtersCondition.add(equal("status", status)));
    }

    private void addSearchByClientCondition() {
        invoices_ClientComboBox.getOptionalValue().ifPresent(client ->
                filtersCondition.add(equal("client", client)));
    }

    private void addSearchByOrderCondition() {
        invoices_OrderComboBox.getOptionalValue().ifPresent(order ->
                filtersCondition.add(equal("order", order)));
    }

    private void addDateRangeConditions() {
        addSearchByFromDateCondition();
        addSearchByToDateCondition();
    }

    private void addSearchByFromDateCondition() {
        invoices_FromDatePicker.getOptionalValue().ifPresent(fromDate ->
                filtersCondition.add(greaterOrEqual("date", fromDate)));
    }

    private void addSearchByToDateCondition() {
        invoices_ToDatePicker.getOptionalValue().ifPresent(fromDate ->
                filtersCondition.add(lessOrEqual("date", fromDate)));
    }

    private void registerUrlQueryParametersBinders() {
        List.<HasValue<?, ?>>of(invoices_ClientComboBox, invoices_OrderComboBox, invoices_StatusSelect, invoices_FromDatePicker, invoices_ToDatePicker)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));

        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(getCurrentView())
                .addEnumBinding(InvoiceStatus.class, invoices_StatusSelect)
                .addComboboxBinding(invoices_OrderComboBox, () -> ordersDc.getItems())
                .addComboboxBinding(invoices_ClientComboBox, () -> clientsDc.getItems())
                .addDatePickerBinding(invoices_FromDatePicker)
                .addDatePickerBinding(invoices_ToDatePicker)
                .build();
    }
}