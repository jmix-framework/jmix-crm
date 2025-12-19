package com.company.crm.app.util.init;

import com.company.crm.model.address.Address;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemComment;
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
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.model.user.activity.userprofile.UserProfileUserActivity;
import com.company.crm.model.user.task.UserTask;
import com.company.crm.security.FullAccessRole;
import io.jmix.core.SaveContext;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.UuidProvider;
import io.jmix.data.PersistenceHints;
import io.jmix.security.role.assignment.RoleAssignment;
import io.jmix.security.role.assignment.RoleAssignmentRepository;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.company.crm.app.util.price.PriceCalculator.calculateTotal;

/**
 * Loads demo data on first application startup.
 * If clients table is not empty, does nothing.
 */
@Component
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final UnconstrainedDataManager dataManager;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(RoleAssignmentRepository roleAssignmentRepository, UnconstrainedDataManager dataManager, PasswordEncoder passwordEncoder) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataManager = dataManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent(ApplicationReadyEvent event) {
        initDataIfNeeded();
    }

    public void resetDemoData() {
        log.info("Resetting demo data...");
        clearData();
        initData();
    }

    private void initDataIfNeeded() {
        if (!shouldInitializeDemoData()) return;
        initData();
    }

    private void initData() {
        log.info("Initializing demo data...");

        List<User> users = generateUsers();
        assignRoles(users);
        generateUserTasks(users);

        List<Client> clients = generateClients(60, users);
        generateContacts(clients);

        Map<Category, List<CategoryItem>> catalog = generateCatalog(10, 10);
        List<Order> orders = generateOrders(clients, catalog);

        List<Invoice> invoices = generateInvoices(orders);
        generatePayments(invoices);

        generateUserActivities(users, clients, orders);

        log.info("Demo data initialization finished: " +
                        "categories={}, categoriesItems={}, " +
                        "clients={} contacts={} orders={} " +
                        "invoices={} payments={}",
                catalog.size(),
                catalog.values().stream().mapToLong(Collection::size).sum(),
                clients.size(),
                dataManager.loadValue("select count(c) from Contact c", Long.class).one(),
                dataManager.loadValue("select count(o) from Order_ o", Long.class).one(),
                dataManager.loadValue("select count(i) from Invoice i", Long.class).one(),
                dataManager.loadValue("select count(p) from Payment p", Long.class).one()
        );
    }

    private void clearData() {
        log.info("Clearing data...");
        var entityClassesToRemove = List.of(
                Payment.class,
                Invoice.class,
                Order.class,
                Contact.class,
                Client.class,
                RoleAssignmentEntity.class,
                UserProfileUserActivity.class,
                ClientUserActivity.class,
                CategoryItem.class,
                Category.class,
                User.class
        );

        for (Class<?> entityClass : entityClassesToRemove) {
            List<?> entitiesToRemove = dataManager.load(entityClass).all().list();
            filterEntitiesToRemove(entityClass, entitiesToRemove);
            log.info("Removing {} entities of type {}", entitiesToRemove.size(), entityClass.getSimpleName());
            SaveContext saveContext = new SaveContext()
                    .setDiscardSaved(true)
                    .setHint(PersistenceHints.SOFT_DELETION, false);
            saveContext.removing(entitiesToRemove.toArray());
            dataManager.save(saveContext);
        }
    }

    private void filterEntitiesToRemove(Class<?> entityClass, List<?> entities) {
        if (entityClass.equals(User.class)) {
            excludeAdmins(entities);
        } else if (entityClass.equals(RoleAssignmentEntity.class)) {
            excludeAdminAssignments(entities);
        }
    }

    private void excludeAdmins(List<?> entities) {
        for (Object entity : new ArrayList<>(entities)) {
            if (entity instanceof User user) {
                Collection<RoleAssignment> userRoles =
                        roleAssignmentRepository.getAssignmentsByUsername(user.getUsername());
                if (userRoles.stream().anyMatch(ra -> ra.getRoleCode().equals(FullAccessRole.CODE))) {
                    entities.remove(user);
                }
            }
        }
    }

    private void excludeAdminAssignments(List<?> entities) {
        for (Object entity : new ArrayList<>(entities)) {
            if (entity instanceof RoleAssignmentEntity roleAssignment
                    && roleAssignment.getRoleCode().equals(FullAccessRole.CODE)) {
                entities.remove(roleAssignment);
            }
        }
    }

    private Map<Category, List<CategoryItem>> generateCatalog(int categoriesCount, int categoryItemsCount) {
        log.info("Generating catalog with {} categories and {} items per category...", categoriesCount, categoryItemsCount);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        Map<Category, List<CategoryItem>> result = new HashMap<>();
        for (int i = 0; i < categoriesCount; i++) {
            int categoryNumber = i + 1;
            Category category = dataManager.create(Category.class);
            category.setName("Category " + categoryNumber);
            category.setDescription("Category description");
            category.setCode(generateUniqueHumanReadableCode(random) + categoryNumber);
            if (i > 0 && random.nextBoolean()) {
                category.setParent(new ArrayList<>(result.keySet()).get(random.nextInt(result.size())));
            }

            dataManager.saveWithoutReload(category);

            List<CategoryItem> categoryItems = new ArrayList<>();
            for (int j = 0; j < categoryItemsCount; j++) {
                int itemNumber = j + 1;
                CategoryItem categoryItem = dataManager.create(CategoryItem.class);
                categoryItem.setCategory(category);
                categoryItem.setName("Product %d in category %d".formatted(itemNumber, categoryNumber));
                categoryItem.setCode(generateUniqueHumanReadableCode(random) + itemNumber);
                categoryItem.setUom(random.nextBoolean() ? "kg" : "pcs");
                categoryItems.add(dataManager.save(categoryItem));
                if (random.nextBoolean()) categoryItemComment(categoryItem);
            }

            result.put(category, categoryItems);
        }

        return result;
    }

    private String generateUniqueHumanReadableCode(ThreadLocalRandom random) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        int length = 6 + random.nextInt(3); // 6-8 characters
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private boolean shouldInitializeDemoData() {
        Long cnt = dataManager.loadValue("select count(c) from Client c", Long.class).one();
        if (cnt > 0) {
            log.info("Demo data already present ({} clients). Skipping initialization.", cnt);
            return false;
        }
        return true;
    }

    private List<User> generateUsers() {
        log.info("Generating users...");
        return List.of(
                saveUser("alice", "Alice", "Brown"),
                saveUser("james", "James", "Wilson"),
                saveUser("mary", "Mary", "Jones"),
                saveUser("linda", "Linda", "Evans"),
                saveUser("susan", "Susan", "Baker"),
                saveUser("bob", "Robert", "Taylor"),
                saveUser("jared", "Jared", "Glover")
        );
    }

    private void generateUserTasks(List<User> users) {
        log.info("Generating user tasks...");
        ThreadLocalRandom random = ThreadLocalRandom.current();

        var tasks = Map.of(
                "Make report", "Send year finance report to CEO",
                "Client meeting", "Schedule meeting with new client",
                "Update documentation", "Review and update project documentation",
                "Team training", "Organize training session for new team members",
                "Budget review", "Review quarterly budget and expenses",
                "System backup", "Perform system backup and verification",
                "Client presentation", "Prepare presentation for client demo",
                "Code review", "Review pull requests from development team",
                "Risk assessment", "Conduct project risk assessment",
                "Status update", "Send weekly status update to stakeholders"
        );

        users.forEach(user ->
                tasks.forEach((title, description) -> {
                    if (random.nextBoolean()) {
                        LocalDate dueDate = randomDateWithinDays(30, random).toLocalDate();
                        boolean completed = random.nextBoolean();
                        saveUserTask(title, description, dueDate, user, completed);
                    }
                }));
    }

    private void saveUserTask(String title, String description, LocalDate dueDate, User user, boolean completed) {
        UserTask userTask = dataManager.create(UserTask.class);
        userTask.setTitle(title);
        userTask.setDescription(description);
        userTask.setDueDate(dueDate);
        userTask.setAuthor(user);
        userTask.setIsCompleted(completed);
        dataManager.saveWithoutReload(userTask);
    }

    private User saveUser(String username, String firstName, String lastName) {
        User user = dataManager.create(User.class);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(createPassword());
        return dataManager.save(user);
    }

    private void assignRoles(List<User> users) {
        log.info("Assigning roles to users...");
        for (User user : users) {
            boolean isManager = Arrays.asList("alice", "james").contains(user.getUsername());

            RoleAssignmentEntity roleAssignment = dataManager.create(RoleAssignmentEntity.class);
            roleAssignment.setUsername(user.getUsername());
            roleAssignment.setRoleCode(isManager ? "manager" : "employee");
            roleAssignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
            dataManager.saveWithoutReload(roleAssignment);
        }
    }

    private void generateUserActivities(List<User> users, List<Client> clients, List<Order> orders) {
        log.info("Generating user activities...");

        ThreadLocalRandom random = ThreadLocalRandom.current();

        OffsetDateTime now = OffsetDateTime.now();

        clients.forEach(client -> {
            ClientUserActivity userActivity = dataManager.create(ClientUserActivity.class);
            userActivity.setClient(client);
            userActivity.setUser(users.get(random.nextInt(users.size())));
            userActivity.setActionDescription("Update client profile");
            userActivity.setCreatedDate(random.nextBoolean() ? now.minusDays(1) : now);
            dataManager.saveWithoutReload(userActivity);
        });

        orders.forEach(order -> {
            ClientUserActivity userActivity = dataManager.create(ClientUserActivity.class);
            userActivity.setClient(order.getClient());
            userActivity.setUser(users.get(random.nextInt(users.size())));
            userActivity.setActionDescription("Update order " + order.getNumber());
            userActivity.setCreatedDate(random.nextBoolean() ? now.minusDays(1) : now);
            dataManager.saveWithoutReload(userActivity);
        });
    }

    private List<Client> generateClients(int count, List<User> users) {
        log.info("Generating {} clients...", count);

        var faker = new Faker();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Client> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Client client = dataManager.create(Client.class);
            client.setName(faker.company().name());
            client.setFullName(faker.company().name() + " " + faker.company().suffix());
            client.setAddress(createAddressFrom(faker.address()));
            client.setType(ClientType.values()[random.nextInt(ClientType.values().length)]);
            client.setVatNumber(randomVatLike(random));
            client.setRegNumber("REG-" + (1000 + random.nextInt(9000)) + (i % 10));
            client.setWebsite("https://" + faker.internet().domainName());
            client.setAccountManager(users.get(random.nextInt(users.size())));

            result.add(dataManager.save(client));
        }
        return result;
    }

    private Address createAddressFrom(net.datafaker.providers.base.Address fakerAddress) {
        Address address = dataManager.create(Address.class);
        address.setPostalCode(fakerAddress.postcode());
        address.setCountry(fakerAddress.country());
        address.setCity(fakerAddress.city());
        address.setStreet(fakerAddress.streetName());
        address.setBuilding(fakerAddress.buildingNumber());
        address.setApartment(ThreadLocalRandom.current().nextInt(50) + "");
        return address;
    }

    private void generateContacts(List<Client> clients) {
        log.info("Generating contacts...");

        var faker = new Faker();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Contact> toSave = new ArrayList<>();
        for (Client client : clients) {
            int n = random.nextInt(1, 4); // 1..3
            for (int i = 0; i < n; i++) {
                Contact contact = dataManager.create(Contact.class);
                contact.setClient(client);
                String first = faker.name().firstName();
                String last = faker.name().lastName();
                contact.setPerson(first + " " + last);
                contact.setPosition(randomPosition(random));
                LocalDate start = randomDateWithinYears(2, random);
                contact.setStartDate(start);
                if (random.nextBoolean()) {
                    contact.setEndDate(start.plusMonths(random.nextInt(1, 18)));
                }
                String domain = domainFromWebsite(client.getWebsite());
                String email = (slug(first) + "." + slug(last) + "@" + domain).toLowerCase(Locale.ROOT);
                contact.setEmail(email);
                contact.setPhone(faker.phoneNumber().cellPhone());
                toSave.add(contact);
            }
        }
        dataManager.saveWithoutReload(toSave.toArray());
    }

    private List<Order> generateOrders(List<Client> clients, Map<Category, List<CategoryItem>> catalog) {
        log.info("Generating orders...");

        ThreadLocalRandom random = ThreadLocalRandom.current();
        var categoryItems = catalog.values().stream().flatMap(Collection::stream).toList();
        int categoryItemsSize = categoryItems.size();

        List<Order> result = new ArrayList<>();
        SaveContext saveContext = new SaveContext().setDiscardSaved(true);
        for (Client client : clients) {
            int n = random.nextInt(0, 9); // 0..8
            for (int i = 0; i < n; i++) {
                Order order = dataManager.create(Order.class);
                order.setClient(client);
                LocalDate date = randomDateWithinYears(2, random);
                order.setDate(date);
                order.setQuote("Q-" + date.getYear() + "-" + (1000 + random.nextInt(9000)));
                if (random.nextBoolean()) order.setComment(orderComment(random));
                order.setStatus(OrderStatus.values()[random.nextInt(OrderStatus.values().length)]);
                List<OrderItem> orderItems = generateOrderItems(order, categoryItems.subList(0, random.nextInt(1, categoryItemsSize / 3)));
                BigDecimal itemsTotal = order.getItemsTotal();
                if (random.nextInt(4) == 0) {
                    // discount either value or percent
                    if (random.nextBoolean()) {
                        order.setDiscountValue(
                                BigDecimal.valueOf(
                                        random.nextInt(10, itemsTotal.subtract(BigDecimal.ONE).intValue())));
                    } else {
                        order.setDiscountPercent(
                                BigDecimal.valueOf(random.nextInt(1, 30)));
                    }
                }
                order.setTotal(calculateTotal(order));
                saveContext.saving(order, orderItems);
                result.add(order);
            }
        }
        dataManager.save(saveContext);
        return result;
    }

    private List<OrderItem> generateOrderItems(Order order, Collection<CategoryItem> categoryItems) {
        log.info("Generating {} order items for order {}", categoryItems.size(), order.getId());

        ThreadLocalRandom random = ThreadLocalRandom.current();

        var generatedItems = new ArrayList<OrderItem>();
        for (CategoryItem categoryItem : categoryItems) {
            OrderItem orderItem = dataManager.create(OrderItem.class);
            orderItem.setCategoryItem(categoryItem);
            orderItem.setOrder(order);
            orderItem.setVatAmount(BigDecimal.valueOf(random.nextInt(100, 200)));
            orderItem.setNetPrice(BigDecimal.valueOf(random.nextInt(1000, 10000)));
            orderItem.setGrossPrice(BigDecimal.valueOf(random.nextInt(2000, 20000)));
            orderItem.setQuantity(BigDecimal.valueOf(random.nextInt(2, 10)));
            generatedItems.add(orderItem);
        }

        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null) orderItems = new ArrayList<>();
        orderItems.addAll(generatedItems);
        order.setOrderItems(orderItems);
        return generatedItems;
    }

    private List<Invoice> generateInvoices(List<Order> orders) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Invoice> result = new ArrayList<>();
        for (Order order : orders) {
            if (random.nextInt(100) < 70) { // 70% orders have an invoice
                Invoice invoice = dataManager.create(Invoice.class);
                invoice.setClient(order.getClient());
                invoice.setOrder(order);
                LocalDate date = order.getDate() != null ? order.getDate().plusDays(random.nextInt(1, 15)) : randomDateWithinYears(2, random);
                invoice.setDate(date);
                invoice.setDueDate(date.plusDays(random.nextInt(7, 45)));
                BigDecimal subtotal = order.getTotal();
                invoice.setSubtotal(subtotal);
                BigDecimal vat = subtotal.multiply(BigDecimal.valueOf(0.2))
                        .setScale(2, RoundingMode.HALF_UP);
                invoice.setVat(vat);
                invoice.setTotal(subtotal.add(vat));
                invoice.setStatus(InvoiceStatus.values()[random.nextInt(InvoiceStatus.values().length)]);
                result.add(dataManager.save(invoice));
            }
        }
        return result;
    }

    private void generatePayments(List<Invoice> invoices) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Invoice invoice : invoices) {
            int n = random.nextInt(0, 9); // 0..8
            BigDecimal remaining = invoice.getTotal();
            for (int i = 0; i < n && remaining.compareTo(BigDecimal.ZERO) > 0; i++) {
                Payment payment = dataManager.create(Payment.class);
                payment.setInvoice(invoice);
                LocalDate date = (invoice.getDate() != null ? invoice.getDate() : randomDateWithinYears(2, random)).plusDays(random.nextInt(1, 60));
                payment.setDate(date);
                BigDecimal part = remaining.multiply(BigDecimal.valueOf(0.25 + random.nextDouble(0.5))).setScale(2, BigDecimal.ROUND_HALF_UP);
                if (part.compareTo(remaining) > 0) part = remaining;
                payment.setAmount(part);
                remaining = remaining.subtract(part);
                dataManager.saveWithoutReload(payment);
            }
        }
    }

    private String createPassword() {
        return passwordEncoder.encode("1");
    }

    private String randomVatLike(ThreadLocalRandom r) {
        String[] cc = {"US", "GB", "DE", "FR", "CA", "AU", "IE", "NL", "SE", "NO", "ES", "IT", "PL", "JP", "SG"};
        String country = cc[r.nextInt(cc.length)];
        int part1 = 10 + r.nextInt(89);
        int part2 = 1000000 + r.nextInt(9000000);
        return country + part1 + "-" + part2;
    }


    private String randomPosition(ThreadLocalRandom r) {
        String[] pos = {"CTO", "CIO", "Head of Procurement", "Operations Manager", "HR Lead", "Finance Manager", "IT Specialist", "Project Manager"};
        return pos[r.nextInt(pos.length)];
    }

    private CategoryItemComment categoryItemComment(CategoryItem categoryItem) {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        String[] messages = {
                "Very good quality",
                "Nice product",
                "Better quality and price than others",
                "You never try something like this before",
                "Quality is our priority",
                "Our services are the best ever",
                "Many businesses recommend our service",
        };

        CategoryItemComment comment = dataManager.create(CategoryItemComment.class);
        comment.setCategoryItem(categoryItem);
        comment.setMessage(messages[r.nextInt(messages.length)]);

        // Save the comment first to avoid cascade persistence issues
        dataManager.save(comment);

        List<CategoryItemComment> comments = categoryItem.getComments();
        if (comments == null) comments = new ArrayList<>();
        comments.add(comment);
        categoryItem.setComments(comments);

        return comment;
    }

    private String orderComment(ThreadLocalRandom r) {
        String[] comments = {
                "Urgent delivery requested.",
                "Include extended warranty.",
                "Customer asked for bulk discount.",
                "Repeat order based on last year's contract.",
                "Requires onsite installation.",
                "Custom branding needed.",
                "Ship in two batches.",
                "Coordinate with procurement before invoicing."
        };
        return comments[r.nextInt(comments.length)];
    }

    private LocalDate randomDateWithinYears(int years, ThreadLocalRandom r) {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusYears(years);
        long days = now.toEpochDay() - start.toEpochDay();
        return start.plusDays(r.nextLong(days + 1));

    }

    private OffsetDateTime randomDateWithinDays(int days, ThreadLocalRandom r) {
        return OffsetDateTime.now()
                .minusDays(days)
                .withHour(r.nextInt(12))
                .withMinute(r.nextInt(60))
                .withSecond(r.nextInt(60));
    }

    private String slug(String s) {
        if (s == null) return "user";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String domainFromWebsite(String site) {
        if (site == null || site.isBlank()) return "example.com";
        String d = site.replaceFirst("https?://", "");
        int idx = d.indexOf('/');
        return idx > 0 ? d.substring(0, idx) : d;
    }
}
