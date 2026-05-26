package com.company.crm.ai.context;

import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemComment;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import com.company.crm.model.user.task.UserTask;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * Registry of supported CRM context entities for AI interactions,
 * providing Vaadin icons, message keys, and Jmix FetchPlans.
 */
public enum AiContextEntityDefinition {

    USER_TASK(UserTask.class, "task", VaadinIcon.CHECK_SQUARE, "contextEntity.tasks", true, true,
            fp -> fp.builder(UserTask.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("author", FetchPlan.BASE)
                    .build()),

    CATEGORY(Category.class, "category", VaadinIcon.LIST_UL, "contextEntity.categories", true, true,
            fp -> fp.builder(Category.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("parent", FetchPlan.BASE)
                    .build()),

    CATEGORY_ITEM(CategoryItem.class, "product", VaadinIcon.CUBES, "contextEntity.products", true, true,
            fp -> fp.builder(CategoryItem.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("category", FetchPlan.BASE)
                    .add("comments", comment -> comment.addFetchPlan(FetchPlan.BASE)
                            .add("sender", FetchPlan.BASE))
                    .build()),

    CLIENT(Client.class, "client", VaadinIcon.USERS, "contextEntity.clients", true, true,
            fp -> fp.builder(Client.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("accountManager", FetchPlan.INSTANCE_NAME)
                    .add("contacts", contact -> contact.addFetchPlan(FetchPlan.BASE))
                    .add("orders", order -> order
                            .add("number")
                            .add("date")
                            .add("total")
                            .add("status")
                            .add("orderItems", orderItem -> orderItem.addFetchPlan(FetchPlan.BASE)
                                    .add("categoryItem", categoryItem -> categoryItem.addFetchPlan(FetchPlan.BASE)
                                            .add("category", FetchPlan.BASE))))
                    .add("invoices", invoice -> invoice
                            .add("number")
                            .add("date")
                            .add("dueDate")
                            .add("total")
                            .add("status")
                            .add("order", FetchPlan.INSTANCE_NAME)
                            .add("payments", payment -> payment
                                    .add("amount")
                                    .add("date")))
                    .build()),

    ORDER(Order.class, "order", VaadinIcon.CART, "contextEntity.orders", true, true,
            fp -> fp.builder(Order.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("client", FetchPlan.BASE)
                    .add("orderItems", orderItem -> orderItem.addFetchPlan(FetchPlan.BASE)
                            .add("categoryItem", categoryItem -> categoryItem.addFetchPlan(FetchPlan.BASE)
                                    .add("category", FetchPlan.BASE)))
                    .add("invoices", invoice -> invoice.addFetchPlan(FetchPlan.BASE)
                            .add("payments", FetchPlan.BASE))
                    .build()),

    INVOICE(Invoice.class, "invoice", VaadinIcon.FILE_TEXT, "contextEntity.invoices", true, true,
            fp -> fp.builder(Invoice.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("client", FetchPlan.BASE)
                    .add("order", order -> order.addFetchPlan(FetchPlan.BASE)
                            .add("orderItems", orderItem -> orderItem.addFetchPlan(FetchPlan.BASE)
                                    .add("categoryItem", FetchPlan.BASE)))
                    .add("payments", FetchPlan.BASE)
                    .build()),

    PAYMENT(Payment.class, "payment", VaadinIcon.WALLET, "contextEntity.payments", true, true,
            fp -> fp.builder(Payment.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("invoice", invoice -> invoice.addFetchPlan(FetchPlan.BASE)
                            .add("client", FetchPlan.BASE)
                            .add("order", FetchPlan.BASE))
                    .build()),

    CONTACT(Contact.class, "contact", VaadinIcon.USER_CARD, "contextEntity.contacts", false, true,
            fp -> fp.builder(Contact.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("client", FetchPlan.BASE)
                    .build()),

    /**
     * Order items are useful for AI tools to query individual positions of an order,
     * but are not directly select-able as a top-level chat context by the user in the UI.
     * Therefore, we configure {@code addMenuVisible = false} and {@code toolsAllowed = true}.
     */
    ORDER_ITEM(OrderItem.class, "orderItem", VaadinIcon.CART_O, "contextEntity.orderItems", false, true,
            fp -> fp.builder(OrderItem.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("order", order -> order.addFetchPlan(FetchPlan.BASE)
                            .add("client", FetchPlan.BASE))
                    .add("categoryItem", categoryItem -> categoryItem.addFetchPlan(FetchPlan.BASE)
                            .add("category", FetchPlan.BASE))
                    .build()),

    CATEGORY_ITEM_COMMENT(CategoryItemComment.class, "productComment", VaadinIcon.COMMENT, "contextEntity.productComments", false, true,
            fp -> fp.builder(CategoryItemComment.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("categoryItem", categoryItem -> categoryItem.addFetchPlan(FetchPlan.BASE)
                            .add("category", FetchPlan.BASE))
                    .add("sender", FetchPlan.BASE)
                    .build()),

    USER(User.class, "user", VaadinIcon.USER, "contextEntity.users", false, true,
            fp -> fp.builder(User.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .build());

    private final Class<? extends UuidEntity> entityClass;
    private final String suggestionKey;
    private final VaadinIcon icon;
    private final String menuMessageKey;
    private final boolean addMenuVisible;
    private final boolean toolsAllowed;
    private final Function<FetchPlans, FetchPlan> fetchPlanFactory;

    AiContextEntityDefinition(
            Class<? extends UuidEntity> entityClass,
            String suggestionKey,
            VaadinIcon icon,
            String menuMessageKey,
            boolean addMenuVisible,
            boolean toolsAllowed,
            Function<FetchPlans, FetchPlan> fetchPlanFactory
    ) {
        this.entityClass = entityClass;
        this.suggestionKey = suggestionKey;
        this.icon = icon;
        this.menuMessageKey = menuMessageKey;
        this.addMenuVisible = addMenuVisible;
        this.toolsAllowed = toolsAllowed;
        this.fetchPlanFactory = fetchPlanFactory;
    }

    public Class<? extends UuidEntity> entityClass() {
        return entityClass;
    }

    public String suggestionKey() {
        return suggestionKey;
    }

    public VaadinIcon icon() {
        return icon;
    }

    public String menuMessageKey() {
        return menuMessageKey;
    }

    public boolean addMenuVisible() {
        return addMenuVisible;
    }

    public boolean toolsAllowed() {
        return toolsAllowed;
    }

    public FetchPlan fetchPlan(FetchPlans fetchPlans) {
        return fetchPlanFactory.apply(fetchPlans);
    }

    public static Optional<AiContextEntityDefinition> findByEntityClass(Class<?> entityClass) {
        if (entityClass == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(definition -> definition.entityClass().isAssignableFrom(entityClass))
                .findFirst();
    }
}
