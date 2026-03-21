package com.company.crm.util;

import com.company.crm.model.address.Address;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import io.jmix.core.UnconstrainedDataManager;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@TestComponent
public class Entities {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private final PasswordEncoder passwordEncoder;
    private final UnconstrainedDataManager dataManager;

    public Entities(PasswordEncoder passwordEncoder, UnconstrainedDataManager dataManager) {
        this.passwordEncoder = passwordEncoder;
        this.dataManager = dataManager;
    }

    public User user() {
        return user(UniqueValues.string());
    }

    public User user(String name) {
        return createAndSaveEntity(User.class, newUser -> {
            newUser.setUsername(name);
            newUser.setPassword(passwordEncoder.encode("test"));
        });
    }

    public Client client() {
        return client(UniqueValues.string(), randomClientType());
    }

    public Client client(String name) {
        return client(name, randomClientType());
    }

    public Client client(String name, ClientType type) {
        return createAndSaveEntity(Client.class, client -> {
            client.setName(name);
            client.setType(type);
            client.setAddress(address());
        });
    }

    public Client client(String name, int daysAgo) {
        return client(name, daysAgo, randomClientType());
    }

    public Client client(String name, int daysAgo, ClientType type) {
        Client client = createAndSaveEntity(Client.class, c -> {
            c.setName(name);
            c.setType(type);
            c.setAddress(address());
        });
        client.setCreatedDate(
                LocalDate.now().minusDays(daysAgo)
                        .atStartOfDay()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toOffsetDateTime());
        return saveWithoutReload(client);
    }

    public Order order(Client client, LocalDate date, OrderStatus status) {
        return createAndSaveEntity(Order.class, order -> {
            order.setClient(client);
            order.setDate(date);
            order.setStatus(status);
        });
    }

    public Order order(Client client, String orderNumber, LocalDate date, com.company.crm.model.order.OrderStatus status, BigDecimal total) {
        Order order = createEntity(Order.class);
        order.setClient(client);
        order.setNumber(orderNumber);
        order.setDate(date);
        order.setStatus(status);
        order.setTotal(total);
        return saveWithoutReload(order);
    }

    public Order order(Client client, LocalDate date, com.company.crm.model.order.OrderStatus status, BigDecimal total) {
        return createAndSaveEntity(Order.class, order -> {
            order.setClient(client);
            order.setDate(date);
            order.setStatus(status);
            order.setTotal(total);
        });
    }

    public Invoice invoice(Client client, Order order) {
        return createAndSaveEntity(Invoice.class, invoice -> {
            invoice.setClient(client);
            invoice.setOrder(order);
        });
    }

    public Payment payment(Invoice invoice, LocalDate date) {
        return createAndSaveEntity(Payment.class, payment -> {
            payment.setInvoice(invoice);
            payment.setDate(date);
            payment.setAmount(BigDecimal.TEN);
        });
    }


    public Invoice invoice(Client client, Order order, BigDecimal total, InvoiceStatus status, LocalDate invoiceDate) {
        return createAndSaveEntity(Invoice.class, invoice -> {
            invoice.setClient(client);
            invoice.setOrder(order);
            invoice.setTotal(total);
            invoice.setStatus(status);
            invoice.setDate(invoiceDate);
        });
    }

    public Payment payment(Invoice invoice, LocalDate date, BigDecimal amount) {
        return createAndSaveEntity(Payment.class, payment -> {
            payment.setInvoice(invoice);
            payment.setDate(date);
            payment.setAmount(amount);
        });
    }

    public Contact contact(Client client, String person, String position) {
        return createAndSaveEntity(Contact.class, contact -> {
            contact.setClient(client);
            contact.setPerson(person);
            contact.setPosition(position);
            contact.setStartDate(LocalDate.now().minusMonths(6));
        });
    }

    public Category category(String name, String code) {
        return createAndSaveEntity(Category.class, c -> {
            c.setName(name);
            c.setCode(code);
        });
    }

    public CategoryItem categoryItem(String name, String code, Category category, BigDecimal price, UomType uom) {
        return createAndSaveEntity(CategoryItem.class, i -> {
            i.setName(name);
            i.setCode(code);
            i.setCategory(category);
            i.setPrice(price);
            i.setUom(uom);
        });
    }

    public OrderItem orderItem(Order order, CategoryItem item, BigDecimal quantity) {
        return createAndSaveEntity(OrderItem.class, oi -> {
            oi.setOrder(order);
            oi.setCategoryItem(item);
            oi.setQuantity(quantity);
            oi.setNetPrice(item.getPrice());
            oi.setGrossPrice(item.getPrice());
        });
    }

    public Address address() {
        Address address = dataManager.create(Address.class);
        address.setCountry("Germany");
        address.setCity("Munich");
        address.setStreet("Leopoldstraße");
        address.setBuilding(String.valueOf(RANDOM.nextInt(1, 200)));
        address.setPostalCode("80802");
        address.setApartment(String.valueOf(RANDOM.nextInt(1, 50)));
        return address;
    }

    public <E> E createAndSaveEntity(Class<E> entityClass, Consumer<E> creation) {
        E entity = createEntity(entityClass);
        creation.accept(entity);
        return saveWithoutReload(entity);
    }

    public <E> E createEntity(Class<E> entityClass) {
        return dataManager.create(entityClass);
    }

    public <E> E saveWithoutReload(E entity) {
        dataManager.save(entity);
        return entity;
    }

    private static ClientType randomClientType() {
        return ClientType.values()[RANDOM.nextInt(ClientType.values().length)];
    }
}
