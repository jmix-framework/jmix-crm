package com.company.crm.view.tasks;

import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "tasks", layout = MainView.class)
@ViewController(id = "TasksView")
@ViewDescriptor(path = "tasks-view.xml")
public class TasksView extends StandardView {
}