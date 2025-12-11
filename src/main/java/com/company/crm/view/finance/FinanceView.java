package com.company.crm.view.finance;

import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.view.invoice.InvoiceListView;
import com.company.crm.view.main.MainView;
import com.company.crm.view.payment.PaymentListView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Views;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "finance-view", layout = MainView.class)
@ViewController(id = "FinanceView")
@ViewDescriptor(path = "finance-view.xml")
public class FinanceView extends StandardView {

    @Autowired
    private Views views;

    @ViewComponent
    private JmixTabSheet tabSheet;

    @Subscribe
    private void onInit(final InitEvent event) {
        InvoiceListView invoiceListView = views.create(InvoiceListView.class);
        PaymentListView paymentListView = views.create(PaymentListView.class);
        paymentListView.addAttachListener(e -> paymentListView.loadData());

        tabSheet.add("Invoices", invoiceListView);
        tabSheet.add("Payments", paymentListView);

        TabIndexUrlQueryParameterBinder.register(this, tabSheet);
    }
}