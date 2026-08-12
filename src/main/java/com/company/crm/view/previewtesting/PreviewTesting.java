package com.company.crm.view.previewtesting;


import com.company.crm.view.main.MainView;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "preview-testing", layout = MainView.class)
@ViewController(id = "PreviewTesting")
@ViewDescriptor(path = "preview-testing.xml")
public class PreviewTesting extends StandardView {
}