package com.company.crm.security;

import com.company.crm.app.util.constant.CrmConstants.ViewIds;
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
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

import static com.company.crm.app.util.constant.CrmConstants.ViewIds.CATEGORY_DETAIL;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.CATEGORY_ITEM_DETAIL;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.CATEGORY_ITEM_LIST;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.CATEGORY_LIST;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.CLIENT_DETAIL;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.CLIENT_LIST;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.FINANCE;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.INVOICE_DETAIL;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.INVOICE_LIST;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.ORDER_DETAIL;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.ORDER_ITEM_DETAIL;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.ORDER_LIST;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.PAYMENT_DETAIL;
import static com.company.crm.app.util.constant.CrmConstants.ViewIds.PAYMENT_LIST;

@ResourceRole(name = "Manager", code = ManagerRole.CODE, scope = "UI")
public interface ManagerRole extends UiMinimalRole {
    String CODE = "manager";

    @MenuPolicy(menuIds = {"home", "categories", "categoryItems", "clients", "orders", "finance"})
    @ViewPolicy(viewIds = {ViewIds.HOME, CATEGORY_LIST, CATEGORY_ITEM_LIST, CLIENT_LIST,
            ORDER_LIST, FINANCE, CATEGORY_ITEM_DETAIL, CATEGORY_DETAIL,
            CLIENT_DETAIL, INVOICE_DETAIL, INVOICE_LIST, ORDER_ITEM_DETAIL, ORDER_DETAIL,
            PAYMENT_DETAIL, PAYMENT_LIST})
    void screens();

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
    @EntityAttributePolicy(entityClass = Client.class, attributes = {"id", "createdBy", "createdDate", "updatedBy", "updatedDate", "version", "deletedBy", "deletedDate", "name", "invoices", "orders", "fullName", "address", "type", "vatNumber", "regNumber", "website", "contacts"}, action = EntityAttributePolicyAction.MODIFY)
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
}