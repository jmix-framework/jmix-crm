package com.company.crm.view.finance;


import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "finance-view", layout = MainView.class)
@ViewController(id = "FinanceView")
@ViewDescriptor(path = "finance-view.xml")
public class FinanceView extends StandardView {
}