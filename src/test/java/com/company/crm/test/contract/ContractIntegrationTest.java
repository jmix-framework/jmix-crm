package com.company.crm.test.contract;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.contract.Contract;
import com.company.crm.model.user.User;
import com.company.crm.security.role.ManagerRole;
import com.company.crm.security.role.OnlyMyAccountsRole;
import com.company.crm.security.role.UiMinimalRole;
import com.company.crm.util.UniqueValues;
import io.jmix.core.FetchPlan;
import io.jmix.core.UnconstrainedDataManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractIntegrationTest extends AbstractTest {

    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;

    @Test
    void managerCreatesAndReadsContract() {
        Client client = entities.client();
        String number = UniqueValues.string();

        UUID id = withManager(() -> {
            Contract contract = newContract(client, number);
            contract.setAmount(new BigDecimal("1500.00"));
            return dataManager.save(contract).getId();
        });

        Contract loaded = withManager(() -> dataManager.load(Contract.class)
                .id(id)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("client"))
                .one());
        assertThat(loaded.getNumber()).isEqualTo(number);
        assertThat(loaded.getClient()).isEqualTo(client);
        assertThat(loaded.getSignedDate()).isEqualTo(LocalDate.now());
        assertThat(loaded.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(loaded.getCreatedBy()).isEqualTo(testUsers.manager().getUsername());
    }

    @Test
    void contractWithNegativeAmountIsNotSaved() {
        Client client = entities.client();
        String number = UniqueValues.string();

        // JPA bean validation (validation mode AUTO) rejects the row on prePersist
        ConstraintViolationException exception = withManager(() -> {
            Contract contract = newContract(client, number);
            contract.setAmount(new BigDecimal("-1.00"));
            return assertThrows(ConstraintViolationException.class, () -> dataManager.save(contract));
        });

        assertThat(exception.getConstraintViolations())
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("amount");
        assertThat(unconstrainedDataManager.load(Contract.class)
                .query("e.number = ?1", number)
                .optional())
                .isEmpty();
    }

    @Test
    void userWithOnlyUiMinimalRoleGetsEmptyContractList() {
        unconstrainedDataManager.saveWithoutReload(newContract(entities.client(), UniqueValues.string()));
        User user = testUsers.ensureUser("contract-ui-minimal-user");
        testUsers.assignRole(user.getUsername(), UiMinimalRole.CODE);

        List<Contract> visibleToUiMinimal = withUser(user, () -> dataManager.load(Contract.class).all().list());
        List<Contract> visibleToManager = withManager(() -> dataManager.load(Contract.class).all().list());

        assertThat(visibleToUiMinimal).isEmpty();
        assertThat(visibleToManager).hasSize(1);
    }

    @Test
    void onlyMyAccountsRoleLimitsContractsToOwnClients() {
        User accountManager = testUsers.ensureUser("contract-account-manager");
        testUsers.assignRole(accountManager.getUsername(), ManagerRole.CODE);
        testUsers.assignRowLevelRole(accountManager.getUsername(), OnlyMyAccountsRole.CODE);
        Client ownClient = entities.createAndSaveEntity(Client.class, client -> {
            client.setName(UniqueValues.string());
            client.setType(ClientType.BUSINESS);
            client.setAddress(entities.address());
            client.setAccountManager(accountManager);
        });
        Contract ownContract = unconstrainedDataManager.save(newContract(ownClient, UniqueValues.string()));
        unconstrainedDataManager.saveWithoutReload(newContract(entities.client(), UniqueValues.string()));

        List<Contract> visible = withUser(accountManager, () -> dataManager.load(Contract.class).all().list());

        assertThat(visible).containsExactly(ownContract);
    }

    @Test
    void deletingClientSoftDeletesItsContracts() {
        Client client = entities.client();
        Contract contract = unconstrainedDataManager.save(newContract(client, UniqueValues.string()));

        unconstrainedDataManager.remove(client);

        assertThat(unconstrainedDataManager.load(Contract.class).id(contract.getId()).optional()).isEmpty();
    }

    private Contract newContract(Client client, String number) {
        Contract contract = unconstrainedDataManager.create(Contract.class);
        contract.setClient(client);
        contract.setNumber(number);
        contract.setSignedDate(LocalDate.now());
        return contract;
    }
}
