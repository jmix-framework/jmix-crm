package com.company.crm.view.contract;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.contract.Contract;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

import static com.company.crm.view.contract.ContractDetailView.ROUTE;

@Route(value = ROUTE, layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CONTRACT_DETAIL)
@ViewDescriptor(path = "contract-detail-view.xml")
@EditedEntityContainer("contractDc")
public class ContractDetailView extends StandardDetailView<Contract> {

    public static final String ROUTE = "contracts/:id";
}
