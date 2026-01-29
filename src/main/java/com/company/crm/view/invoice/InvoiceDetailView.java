package com.company.crm.view.invoice;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.service.settings.CrmSettingsService;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.report.CrmReportUtils;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceRepository;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.company.crm.app.util.price.PriceCalculator.calculateInvoiceFieldsFromOrder;

@Route(value = "invoices/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.INVOICE_DETAIL)
@ViewDescriptor(path = "invoice-detail-view.xml")
@EditedEntityContainer("invoiceDc")
public class InvoiceDetailView extends StandardDetailView<Invoice> {

    @Autowired
    private EntityStates entityStates;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private CrmReportUtils crmReportUtils;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private CrmSettingsService crmSettingsService;

    @ViewComponent
    private JmixButton downloadButton;
    @ViewComponent
    private TypedDatePicker<LocalDate> dueDateField;
    @ViewComponent
    private EntityComboBox<Client> clientsComboBox;
    @ViewComponent
    private EntityComboBox<Order> ordersComboBox;
    @ViewComponent
    private CollectionLoader<Order> ordersDl;

    private boolean orderChangeForbidden = false;

    public void forbidChangeOrder() {
        orderChangeForbidden = true;
        ordersComboBox.setReadOnly(true);
        clientsComboBox.setReadOnly(true);
    }

    @Subscribe
    private void onInitEntity(final InitEntityEvent<Invoice> event) {
        Invoice invoice = event.getEntity();
        Order order = invoice.getOrder();

        invoice.setDate(dateTimeService.now().toLocalDate());
        invoice.setStatus(InvoiceStatus.NEW);

        calculateFieldsFromOrderIfPossible();

        ordersComboBox.setReadOnly(order != null || orderChangeForbidden);
        clientsComboBox.setReadOnly(invoice.getClient() != null || orderChangeForbidden);
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        Invoice invoice = getEditedEntity();
        Order order = invoice.getOrder();

        if (order != null) {
            clientsComboBox.setValue(order.getClient());
            if (invoice.getDueDate() == null) {
                dueDateField.focus();
            }
        } else {
            loadOrders(invoice.getClient());
            ordersComboBox.focus();
        }

        downloadButton.setEnabled(!entityStates.isNew(getEditedEntity()));
    }

    private void loadOrders(@Nullable Client client) {
        if (client == null) {
            ordersDl.setQuery("select e from Order_ e order by e.number asc");
            ordersDl.removeParameter("client");
        } else {
            ordersDl.setQuery("select e from Order_ e where e.client = :client order by e.number asc");
            ordersDl.setParameter("client", client);
        }
        ordersDl.load();
    }

    @Install(to = "invoiceDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Invoice> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return invoiceRepository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(invoiceRepository.save(getEditedEntity()));
    }

    @Subscribe("downloadAction")
    private void onDownloadAction(final ActionPerformedEvent event) {
        crmReportUtils.runAndDownloadReport(getEditedEntity());
    }

    @Subscribe("ordersComboBox.entityLookupAction")
    private void onOrdersComboBoxEntityLookupAction(final ActionPerformedEvent event) {
        dialogWindows.detail(this, Order.class)
                .withInitializer(order -> order.setClient(getEditedEntity().getClient()))
                .open();
    }

    @Subscribe("ordersComboBox")
    private void onOrdersComboBoxComponentValueChange(final ComponentValueChangeEvent<EntityComboBox<Order>, Order> event) {
        Order order = event.getValue();
        if (order != null) {
            clientsComboBox.setValue(order.getClient());
            calculateFieldsFromOrderIfPossible();
        }
    }

    @Subscribe("clientsComboBox")
    private void onClientsComboBoxComponentValueChange(final ComponentValueChangeEvent<EntityComboBox<Client>, Client> event) {
        Client client = event.getValue();
        ordersComboBox.getOptionalValue().ifPresent(order -> {
            if (!Objects.equals(order.getClient(), client)) {
                ordersComboBox.clear();
            }
        });
        loadOrders(client);
    }

    private void calculateFieldsFromOrderIfPossible() {
        Invoice invoice = getEditedEntity();
        Order order = invoice.getOrder();
        if (order != null) {
            calculateInvoiceFieldsFromOrder(order, invoice, crmSettingsService.getDefaultVatPercent());
        }
    }
}