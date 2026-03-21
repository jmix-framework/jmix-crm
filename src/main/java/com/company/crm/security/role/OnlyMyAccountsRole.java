package com.company.crm.security.role;

import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;

@RowLevelRole(name = "Only My Accounts", code = OnlyMyAccountsRole.CODE)
public interface OnlyMyAccountsRole {
    String CODE = "only-my-accounts-rl";

    @JpqlRowLevelPolicy(entityClass = Client.class,
            where = "{E}.accountManager.id = :current_user_id")
    void client();

    @JpqlRowLevelPolicy(entityClass = Order.class,
            where = "{E}.client.accountManager.id = :current_user_id")
    void order();

    @JpqlRowLevelPolicy(entityClass = Invoice.class,
            where = "{E}.client.accountManager.id = :current_user_id")
    void invoice();

    @JpqlRowLevelPolicy(entityClass = Contact.class,
            where = "{E}.client.accountManager.id = :current_user_id")
    void contact();

    @JpqlRowLevelPolicy(entityClass = Payment.class,
            where = "{E}.invoice.client.accountManager.id = :current_user_id")
    void payment();

    @JpqlRowLevelPolicy(entityClass = OrderItem.class,
            where = "{E}.order.client.accountManager.id = :current_user_id")
    void orderItem();

    @JpqlRowLevelPolicy(entityClass = ClientUserActivity.class,
            where = "{E}.client.accountManager.id = :current_user_id")
    void clientUserActivity();
}
