package com.company.crm.security.role;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.contract.Contract;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Contract entity, Contract.list/Contract.detail and the "contracts" menu item.
 * Not a standalone role: Client read access (the client combobox and column) and the
 * filter-condition views come from {@link ManagerRole}, which extends this role.
 */
@ResourceRole(name = ContractManagerRole.NAME, code = ContractManagerRole.CODE)
public interface ContractManagerRole {

    String CODE = "contract-manager";
    String NAME = "Contract manager";

    @EntityAttributePolicy(entityClass = Contract.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Contract.class, actions = EntityPolicyAction.ALL)
    void contract();

    @MenuPolicy(menuIds = "contracts")
    @ViewPolicy(viewIds = {CrmConstants.ViewIds.CONTRACT_LIST, CrmConstants.ViewIds.CONTRACT_DETAIL})
    void contractViews();
}
