package com.company.crm.view.usagehelp;

import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "UsageHelpView", layout = MainView.class)
@ViewController("crm_UsageHelpView")
@ViewDescriptor("usage-help-view.xml")
@DialogMode(closeOnOutsideClick = true, closeOnEsc = true)
public class UsageHelpView extends StandardView {
}
