package com.company.crm.view.pricing;

import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "pricing", layout = MainView.class)
@ViewController(id = "PricingView")
@ViewDescriptor(path = "pricing-view.xml")
public class PricingView extends StandardView {
}