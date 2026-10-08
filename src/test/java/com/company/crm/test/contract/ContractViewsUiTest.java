package com.company.crm.test.contract;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.contract.Contract;
import com.company.crm.util.UniqueValues;
import com.company.crm.util.extenstion.AuthenticatedAs;
import com.company.crm.view.contract.ContractDetailView;
import com.company.crm.view.contract.ContractListView;
import com.vaadin.flow.component.Component;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.ViewControllerUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@AuthenticatedAs(AuthenticatedAs.MANAGER_USERNAME)
class ContractViewsUiTest extends AbstractUiTest {

    @Test
    void contractListViewOpensAndShowsContracts() {
        Contract contract = savedContract();

        ContractListView view = viewTestSupport.navigateTo(ContractListView.class);

        CollectionContainer<Contract> contractsDc = ViewControllerUtils.getViewData(view).getContainer("contractsDc");
        assertThat(contractsDc.getItems()).containsExactly(contract);
    }

    @Test
    void contractDetailViewOpensForExistingContract() {
        Contract contract = savedContract();

        ContractDetailView view = viewTestSupport.navigateToDetailView(Contract.class, contract, ContractDetailView.class);

        TypedTextField<String> numberField = viewTestSupport.getComponent(view, "numberField");
        assertThat(numberField.getTypedValue()).isEqualTo(contract.getNumber());
    }

    @Test
    void contractDetailViewRejectsNegativeAmount() {
        ContractDetailView view = viewTestSupport.navigateToNewEntityDetail(Contract.class, ContractDetailView.class);

        viewTestSupport.setComponentValue("amountField", new BigDecimal("-1.00"));
        ValidationErrors errors = UiTestUtils.validateView(view);

        assertThat(errors.getAll())
                .extracting(error -> Optional.ofNullable(error.getComponent()).flatMap(Component::getId))
                .contains(Optional.of("amountField"));
    }

    private Contract savedContract() {
        return entities.createAndSaveEntity(Contract.class, contract -> {
            contract.setClient(entities.client());
            contract.setNumber(UniqueValues.string());
            contract.setSignedDate(LocalDate.now());
            contract.setAmount(new BigDecimal("250.00"));
        });
    }
}
