package com.company.crm.view.usagehelp;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

import static com.company.crm.view.usagehelp.UsageHelpView.ROUTE;

@ViewDescriptor("usage-help-view.xml")
@ViewController(CrmConstants.ViewIds.USAGE_HELP)
@Route(value = ROUTE, layout = MainView.class)
@DialogMode(closeOnOutsideClick = true, closeOnEsc = true)
public class UsageHelpView extends StandardView {
    public static final String ROUTE = "UsageHelpView";

}
