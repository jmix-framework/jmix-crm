package com.company.crm.view.invoice;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceRepository;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Route(value = "invoices/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.INVOICE_DETAIL)
@ViewDescriptor(path = "invoice-detail-view.xml")
@EditedEntityContainer("invoiceDc")
public class InvoiceDetailView extends StandardDetailView<Invoice> {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private DateTimeService dateTimeService;

    @Subscribe
    private void onInitEntity(final InitEntityEvent<Invoice> event) {
        event.getEntity().setDate(dateTimeService.now().toLocalDate());
        event.getEntity().setStatus(InvoiceStatus.NEW);
    }

    @Install(to = "invoiceDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Invoice> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return invoiceRepository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(invoiceRepository.save(getEditedEntity()));
    }
}