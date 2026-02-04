package com.company.crm.security.role;

import com.company.crm.model.address.Address;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemComment;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.settings.CrmSettings;
import com.company.crm.model.user.User;
import com.company.crm.model.user.activity.UserActivity;
import com.company.crm.model.user.activity.userprofile.UserProfileUserActivity;
import com.company.crm.model.user.task.UserTask;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.model.SecurityScope;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.security.role.annotation.SpecificPolicy;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "Manager", code = ManagerRole.CODE, scope = SecurityScope.UI)
public interface ManagerRole extends UiMinimalRole {

    String CODE = "manager";
    String NAME = "Manager";

    @MenuPolicy(menuIds = {"home", "tasks", "clients", "orders"})
    @ViewPolicy(viewIds = {"HomeView", "UserTask.list", "Client.list", "Order.list", "CategoryItem.detail", "Category.detail", "Client.detail", "Invoice.detail", "Invoice.list", "OrderItem.detail", "Order.detail", "Payment.detail", "Payment.list", "AddressFragment", "Contact.detail", "flowui_AddConditionView", "flowui_GroupFilterCondition.detail", "flowui_JpqlFilterCondition.detail", "flowui_PropertyFilterCondition.detail", "flowui_DateIntervalDialog", "FragmentRenderer"})
    void views();

    @EntityAttributePolicy(entityClass = Address.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Address.class, actions = EntityPolicyAction.ALL)
    void address();

    @EntityAttributePolicy(entityClass = Category.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Category.class, actions = EntityPolicyAction.READ)
    void category();

    @EntityAttributePolicy(entityClass = CategoryItem.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = CategoryItem.class, actions = EntityPolicyAction.READ)
    void categoryItem();

    @EntityAttributePolicy(entityClass = CategoryItemComment.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = CategoryItemComment.class, actions = EntityPolicyAction.READ)
    void categoryItemComment();

    @EntityAttributePolicy(entityClass = Client.class, attributes = "accountManager", action = EntityAttributePolicyAction.VIEW)
    @EntityAttributePolicy(entityClass = Client.class,
            attributes = {"id", "createdBy", "createdDate", "updatedBy", "updatedDate",
                    "version", "deletedBy", "deletedDate", "name", "invoices",
                    "orders", "fullName", "address", "type", "vatNumber",
                    "regNumber", "website", "contacts"},
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Client.class, actions = EntityPolicyAction.ALL)
    void client();

    @EntityAttributePolicy(entityClass = Contact.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Contact.class, actions = EntityPolicyAction.ALL)
    void contact();

    @EntityAttributePolicy(entityClass = Invoice.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Invoice.class, actions = EntityPolicyAction.ALL)
    void invoice();

    @EntityAttributePolicy(entityClass = Order.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Order.class, actions = EntityPolicyAction.ALL)
    void order();

    @EntityAttributePolicy(entityClass = OrderItem.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = OrderItem.class, actions = EntityPolicyAction.ALL)
    void orderItem();

    @EntityAttributePolicy(entityClass = Payment.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Payment.class, actions = EntityPolicyAction.ALL)
    void payment();

    @EntityPolicy(entityClass = UserTask.class, actions = EntityPolicyAction.ALL)
    void userTask();

    @SpecificPolicy(resources = {"ui.genericfilter.modifyConfiguration", "ui.genericfilter.modifyJpqlCondition", "ui.genericfilter.modifyGlobalConfiguration", "datatools.importExportEntity", "datatools.showEntityInfo"})
    void specific();

    @EntityPolicy(entityClass = UserProfileUserActivity.class, actions = EntityPolicyAction.READ)
    void userProfileUserActivity();

    @EntityPolicy(entityClass = UserActivity.class, actions = EntityPolicyAction.READ)
    void userActivity();

    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();

    @EntityPolicy(entityClass = CrmSettings.class, actions = EntityPolicyAction.READ)
    void crmSettings();
}