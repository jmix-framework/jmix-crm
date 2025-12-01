package com.company.crm.view.payment;

import com.company.crm.model.payment.Payment;
import com.company.crm.model.payment.PaymentRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

@Route(value = "payments", layout = MainView.class)
@ViewController(id = "Payment.list")
@ViewDescriptor(path = "payment-list-view.xml")
@LookupComponent("paymentsDataGrid")
@DialogMode(width = "64em")
public class PaymentListView extends StandardListView<Payment> {

    @Autowired
    private PaymentRepository repository;
    @ViewComponent
    private CollectionLoader<Payment> paymentsDl;

    public void loadData() {
        paymentsDl.load();
    }

    @Install(to = "paymentsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Payment> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return repository.findAll(pageable, context).getContent();
    }

    @Install(to = "paymentsDataGrid.removeAction", subject = "delegate")
    private void paymentsDataGridRemoveDelegate(final Collection<Payment> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return repository.count(context);
    }
}