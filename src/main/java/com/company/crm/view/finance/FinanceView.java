package com.company.crm.view.finance;

import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.invoice.InvoicesFragment;
import com.company.crm.view.main.MainView;
import com.company.crm.view.payment.PaymentsFragment;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "finances", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.FINANCE)
@ViewDescriptor(path = "finance-view.xml")
public class FinanceView extends StandardView {

    @Autowired
    private Fragments fragments;

    @ViewComponent
    private JmixTabSheet tabSheet;
    @ViewComponent
    private MessageBundle messageBundle;

    @Subscribe
    private void onInit(final InitEvent event) {
        var invoiceListView = fragments.create(this, InvoicesFragment.class);
        var paymentsFragment = fragments.create(this, PaymentsFragment.class);

        tabSheet.add(messageBundle.getMessage("invoices"), invoiceListView);
        tabSheet.add(messageBundle.getMessage("payments"), paymentsFragment);

        TabIndexUrlQueryParameterBinder.register(this, tabSheet);
    }
}