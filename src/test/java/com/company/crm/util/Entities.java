package com.company.crm.util;

import com.company.crm.model.address.Address;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import io.jmix.core.UnconstrainedDataManager;
import net.datafaker.Faker;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@TestComponent
public class Entities {

    public static final Faker FAKER = new Faker();

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
        return client(UniqueValues.string());
    }

    public Client client(String name) {
        return createAndSaveEntity(Client.class, client -> {
            client.setName(name);
            client.setAddress(address());
        });
    }

    public Order order(Client client, LocalDate date, OrderStatus status) {
        return createAndSaveEntity(Order.class, order -> {
            order.setClient(client);
            order.setDate(date);
            order.setStatus(status);
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
        var fakeAddress = FAKER.address();
        Address address = dataManager.create(Address.class);
        address.setCountry(fakeAddress.country());
        address.setCity(fakeAddress.city());
        address.setStreet(fakeAddress.streetAddress());
        address.setBuilding(fakeAddress.buildingNumber());
        address.setPostalCode(fakeAddress.postcode());
        address.setApartment(RANDOM.nextInt(50) + "");
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
}
