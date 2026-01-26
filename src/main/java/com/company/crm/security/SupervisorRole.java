package com.company.crm.security;

import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemComment;
import com.company.crm.model.client.Client;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = SupervisorRole.NAME, code = SupervisorRole.CODE)
public interface SupervisorRole extends ManagerRole {

    String CODE = "supervisor";
    String NAME = "Supervisor";

    @EntityAttributePolicy(entityClass = Client.class, attributes = "accountManager", action = EntityAttributePolicyAction.MODIFY)
    void client();

    @EntityAttributePolicy(entityClass = Category.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Category.class, actions = EntityPolicyAction.ALL)
    void category();

    @EntityAttributePolicy(entityClass = CategoryItem.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = CategoryItem.class, actions = EntityPolicyAction.ALL)
    void categoryItem();

    @EntityAttributePolicy(entityClass = CategoryItemComment.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = CategoryItemComment.class, actions = EntityPolicyAction.ALL)
    void categoryItemComment();

    @MenuPolicy(menuIds = {"categories", "products"})
    @ViewPolicy(viewIds = {"Category.list", "CategoryItem.list"})
    void screens();
}