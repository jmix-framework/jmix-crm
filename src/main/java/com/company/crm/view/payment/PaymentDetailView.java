package com.company.crm.view.payment;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceRepository;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.payment.PaymentRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Route(value = "payments/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.PAYMENT_DETAIL)
@ViewDescriptor(path = "payment-detail-view.xml")
@EditedEntityContainer("paymentDc")
public class PaymentDetailView extends StandardDetailView<Payment> {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private DialogWindows dialogWindows;

    @ViewComponent
    private EntityComboBox<Invoice> invoiceField;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @Subscribe
    private void onInitEntity(final InitEntityEvent<Payment> event) {
        invoiceField.setReadOnly(event.getEntity().getInvoice() != null);
    }

    @Install(to = "paymentDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Payment> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return paymentRepository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(paymentRepository.save(getEditedEntity()));
    }

    @Subscribe("invoiceField.entityLookupAction")
    private void onInvoiceFieldEntityLookupAction(final ActionPerformedEvent event) {
        dialogWindows.detail(this, Invoice.class)
                .withInitializer(invoice -> {
                    invoice.setOrder(getEditedEntity().getOrder());
                    invoice.setClient(client != null ? client : getEditedEntity().getClient());
                })
                .open();
    }

    @Install(to = "invoicesDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Invoice> invoicesDlLoadFromRepositoryDelegate(final Pageable pageable, final JmixDataRepositoryContext context) {
        if (client != null) {
            return invoiceRepository.findAllByClient(client, pageable);
        } else {
            return invoiceRepository.findAll(pageable, context).getContent();
        }
    }
}