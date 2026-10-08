package com.company.crm.view.contract;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.contract.Contract;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

import static com.company.crm.view.contract.ContractListView.ROUTE;

@Route(value = ROUTE, layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CONTRACT_LIST)
@ViewDescriptor(path = "contract-list-view.xml")
@LookupComponent("contractsDataGrid")
@DialogMode(width = "64em")
public class ContractListView extends StandardListView<Contract> {

    public static final String ROUTE = "contracts";
}
