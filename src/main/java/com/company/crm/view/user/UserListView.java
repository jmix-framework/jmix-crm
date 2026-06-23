package com.company.crm.view.user;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.user.User;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

import static com.company.crm.view.user.UserListView.ROUTE;

@Route(value = ROUTE, layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.USER_LIST)
@ViewDescriptor(path = "user-list-view.xml")
@LookupComponent("usersDataGrid")
@DialogMode(width = "64em", resizable = true)
public class UserListView extends StandardListView<User> {
    public static final String ROUTE = "users";
}