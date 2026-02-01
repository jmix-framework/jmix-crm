package com.company.crm.app.util.init;

import com.company.crm.app.config.SpringProfiles;
import com.company.crm.app.service.catalog.CatalogImportSettings;
import com.company.crm.app.service.catalog.CatalogService;
import com.company.crm.app.service.settings.CrmSettingsService;
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
import com.company.crm.model.user.task.UserTask;
import com.company.crm.security.AdministratorRole;
import com.company.crm.security.ManagerRole;
import com.company.crm.security.SupervisorRole;
import com.company.crm.security.UiMinimalRole;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SystemAuthenticator;
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
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static com.company.crm.app.util.price.PriceCalculator.calculateGrossPrice;
import static com.company.crm.app.util.price.PriceCalculator.calculateInvoiceFieldsFromOrder;
import static com.company.crm.app.util.price.PriceCalculator.calculateNetPrice;
import static com.company.crm.app.util.price.PriceCalculator.calculateTotal;
import static com.company.crm.app.util.price.PriceCalculator.calculateVat;
import static org.apache.commons.collections4.CollectionUtils.union;

/**
 * Generates demo data.
 * If clients table is not empty, does nothing.
 */
@Component
public class DemoDataGenerator implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(DemoDataGenerator.class);

    private static final DemoDataProgressListener NO_OP_PROGRESS = message -> {};

    public static final Map<String, String> USER_TASKS = Map.ofEntries(
            Map.entry("Make report", "Send year finance report to CEO"),
            Map.entry("Client meeting", "Schedule meeting with new client"),
            Map.entry("Update documentation", "Review and update project documentation"),
            Map.entry("Team training", "Organize training session for new team members"),
            Map.entry("Budget review", "Review quarterly budget and expenses"),
            Map.entry("System backup", "Perform system backup and verification"),
            Map.entry("Client presentation", "Prepare presentation for client demo"),
            Map.entry("Code review", "Review pull requests from development team"),
            Map.entry("Risk assessment", "Conduct project risk assessment"),
            Map.entry("Status update", "Send weekly status update to stakeholders"),
            Map.entry("Contract renewal", "Draft contract renewal terms for top clients"),
            Map.entry("Pipeline cleanup", "Archive stale opportunities and update stages"),
            Map.entry("Vendor review", "Evaluate vendor performance metrics for Q2"),
            Map.entry("Security audit", "Coordinate quarterly security audit checks"),
            Map.entry("Inventory check", "Verify stock levels for key items"),
            Map.entry("NPS follow-up", "Call detractors and log feedback"),
            Map.entry("KPI dashboard", "Refresh sales KPI dashboard data"),
            Map.entry("Account health", "Review account health scores and flags"),
            Map.entry("Partner outreach", "Contact partners about co-marketing ideas"),
            Map.entry("Expense approvals", "Process pending expense approval requests")
    );

    private final Messages messages;
    private final Environment environment;
    private final SpringProfiles springProfiles;
    private final CatalogService catalogService;
    private final PasswordEncoder passwordEncoder;
    private final UnconstrainedDataManager dataManager;
    private final SystemAuthenticator systemAuthenticator;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final CrmSettingsService crmSettingsService;
    private final CurrentAuthentication currentAuthentication;
    private final DynamicAttributesInitializer dynamicAttributesInitializer;

    public DemoDataGenerator(RoleAssignmentRepository roleAssignmentRepository,
                             UnconstrainedDataManager dataManager,
                             PasswordEncoder passwordEncoder, Environment environment,
                             CatalogService catalogService, SystemAuthenticator systemAuthenticator, CrmSettingsService crmSettingsService,
                             CurrentAuthentication currentAuthentication, SpringProfiles springProfiles, Messages messages, DynamicAttributesInitializer dynamicAttributesInitializer) {
        this.environment = environment;
        this.dataManager = dataManager;
        this.catalogService = catalogService;
        this.passwordEncoder = passwordEncoder;
        this.systemAuthenticator = systemAuthenticator;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.crmSettingsService = crmSettingsService;
        this.currentAuthentication = currentAuthentication;
        this.springProfiles = springProfiles;
        this.messages = messages;
        this.dynamicAttributesInitializer = dynamicAttributesInitializer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent(ApplicationReadyEvent event) {
        if (springProfiles.isLocalProfile()) {
            initDemoDataIfNeeded();
        }
    }

    public void resetDemoData() {
        log.info("Resetting demo data...");
        clearData();
        initData(NO_OP_PROGRESS);
    }

    public void initDemoDataIfNeeded() {
        initDemoDataIfNeeded(NO_OP_PROGRESS);
    }

    public void initDemoDataIfNeeded(DemoDataProgressListener progressListener) {
        if (!shouldInitializeDemoData()) return;
        DemoDataProgressListener listener = progressListener == null ? NO_OP_PROGRESS : progressListener;
        if (currentAuthentication.isSet()) {
            initData(listener);
        } else {
            systemAuthenticator.runWithSystem(() -> initData(listener));
        }
    }

    private void initData(DemoDataProgressListener progressListener) {
        onInitStart(progressListener);
        initDynamicAttributes(progressListener);

        var users = initUsers(progressListener);
        var clients = initClients(progressListener, users);
        var catalog = initCatalog(progressListener);
        var orders = initOrders(progressListener, clients, catalog);
        var invoices = initInvoices(progressListener, orders);

        initPayments(progressListener, invoices);
        initUserActivities(progressListener, users, clients, orders);
        onInitFinish(progressListener, catalog, clients);
    }

    private void onInitStart(DemoDataProgressListener progressListener) {
        log.info("Initializing demo data...");
        publishProgress(progressListener, "Starting demo data generation");
    }

    private void initDynamicAttributes(DemoDataProgressListener progressListener) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.createDynamicAttributes"));
        dynamicAttributesInitializer.createDynamicAttributesIfNeeded();
    }

    private List<User> initUsers(DemoDataProgressListener progressListener) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.configuring"));

        var users = generateUsers();
        initUsersRoles(progressListener, users);
        initUsersTasks(progressListener, users);

        return users;
    }

    private void initUsersRoles(DemoDataProgressListener progressListener, List<User> users) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.assigningRoles"));
        assignRoles(users);
    }

    private void initUsersTasks(DemoDataProgressListener progressListener, List<User> users) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingTasks"));
        generateUserTasks(users);
    }

    private void initUserActivities(DemoDataProgressListener progressListener, List<User> users, List<Client> clients, List<Order> orders) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingActivities"));
        generateUserActivities(users, clients, orders);
    }

    private Map<Category, List<CategoryItem>> initCatalog(DemoDataProgressListener progressListener) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.importingCatalog"));
        return generateCatalog();
    }

    private List<Client> initClients(DemoDataProgressListener progressListener, List<User> users) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingClients"));

        var clients = generateClients(30, users);
        initClientContacts(progressListener, clients);

        return clients;
    }

    private void initClientContacts(DemoDataProgressListener progressListener, List<Client> clients) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingContacts"));
        generateContacts(clients);
    }

    private List<Order> initOrders(DemoDataProgressListener progressListener, List<Client> clients, Map<Category, List<CategoryItem>> catalog) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.generatingOrders"));
        return generateOrders(clients, catalog);
    }

    private List<Invoice> initInvoices(DemoDataProgressListener progressListener, List<Order> orders) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.generatingInvoices"));
        return generateInvoices(orders);
    }

    private void initPayments(DemoDataProgressListener progressListener, List<Invoice> invoices) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.generatingPayments"));
        generatePayments(invoices);
    }

    private void onInitFinish(DemoDataProgressListener progressListener, Map<Category, List<CategoryItem>> catalog, List<Client> clients) {
        publishProgress(progressListener, messages.getMessage("demoData.progress.finalizing"));

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

    private void publishProgress(DemoDataProgressListener progressListener, String message) {
        try {
            progressListener.onProgress(message);
        } catch (Exception e) {
            log.debug("Ignoring demo data progress update failure", e);
        }
    }

    private void clearData() {
        log.info("Clearing data...");
        var entityClassesToRemove = List.of(
                Client.class,
                Category.class,
                RoleAssignmentEntity.class,
                UserTask.class,
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
                if (userRoles.stream().anyMatch(ra -> ra.getRoleCode().equals(AdministratorRole.CODE))) {
                    entities.remove(user);
                }
            }
        }
    }

    private void excludeAdminAssignments(List<?> entities) {
        for (Object entity : new ArrayList<>(entities)) {
            if (entity instanceof RoleAssignmentEntity roleAssignment
                    && roleAssignment.getRoleCode().equals(AdministratorRole.CODE)) {
                entities.remove(roleAssignment);
            }
        }
    }

    private Map<Category, List<CategoryItem>> generateCatalog() {
        log.info("Generating catalog from catalog.xlsx...");
        try (InputStream inputStream = getClass().getResourceAsStream("/demo-data/catalog.xlsx")) {
            if (inputStream == null) {
                log.error("catalog.xlsx not found in classpath!");
                return Map.of();
            }
            Map<Category, List<CategoryItem>> catalog = catalogService.updateCatalog(new CatalogImportSettings(inputStream));

            ThreadLocalRandom random = ThreadLocalRandom.current();
            catalog.values().forEach(items -> items.forEach(item -> {
                if (random.nextBoolean()) categoryItemComment(item);
            }));

            return catalog;
        } catch (IOException e) {
            log.error("Failed to load catalog.xlsx", e);
            return Map.of();
        }
    }

    private boolean shouldInitializeDemoData() {
        if (!Boolean.parseBoolean(environment.getProperty("crm.generateDemoData", "true"))) {
            log.info("Demo data generation is disabled, skipping...");
            return false;
        }

        Long clientsAmount = dataManager.loadValue("select count(c) from Client c", Long.class).one();
        if (clientsAmount > 0) {
            log.info("Demo data already present ({} clients). Skipping generation....", clientsAmount);
            return false;
        }

        return true;
    }

    private List<User> generateUsers() {
        log.info("Generating users...");
        return List.of(
                saveUser(ManagerRole.CODE.toLowerCase(), "Mike", "Wazowski"),
                saveUser(SupervisorRole.CODE.toLowerCase(), "James", "Sullivan"),
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
        for (User user : union(users, List.of(adminUser()))) {
            generateUserTasks(user);
        }
    }

    private void generateUserTasks(User user) {
        log.info("Generating user tasks for {}...", user.getUsername());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Map.Entry<String, String> entry : USER_TASKS.entrySet()) {
            String title = entry.getKey();
            String description = entry.getValue();
            if (random.nextInt(100) < 40) {
                LocalDate dueDate = randomDateInDays(20, random).toLocalDate();
                boolean completed = random.nextBoolean();
                saveUserTask(title, description, dueDate, user, completed);
            }
        }
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
        user.setPassword(passwordEncoder.encode(username));
        return dataManager.save(user);
    }

    private void assignRoles(List<User> users) {
        log.info("Assigning roles to users...");
        for (User user : users) {
            boolean isSupervisor = Objects.equals(SupervisorRole.CODE.toLowerCase(), user.getUsername());
            boolean isManager = Objects.equals(ManagerRole.CODE.toLowerCase(), user.getUsername());
            String roleCode = isSupervisor ? SupervisorRole.CODE
                    : isManager ? ManagerRole.CODE
                    : UiMinimalRole.CODE;

            RoleAssignmentEntity roleAssignment = dataManager.create(RoleAssignmentEntity.class);
            roleAssignment.setUsername(user.getUsername());
            roleAssignment.setRoleCode(roleCode);
            roleAssignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
            dataManager.saveWithoutReload(roleAssignment);
            log.info("Role [{}] assigned to user [{}]", roleCode, user.getUsername());
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
            userActivity.setActionDescription("%s profile updated".formatted(client.getName()));
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
        if (categoryItemsSize == 0) {
            log.warn("No catalog items found, skipping order generation.");
            return List.of();
        }

        List<Order> result = new ArrayList<>();
        SaveContext saveContext = new SaveContext().setDiscardSaved(true);
        for (Client client : clients) {
            int n = random.nextInt(0, 9); // 0..8
            for (int i = 0; i < n; i++) {
                Order order = dataManager.create(Order.class);
                order.setClient(client);
                LocalDate date = randomDateWithinYears(2, random);
                order.setDate(date);
                if (random.nextBoolean()) order.setComment(orderComment(random));
                order.setStatus(OrderStatus.values()[random.nextInt(OrderStatus.values().length)]);
                int maxItems = Math.max(1, categoryItemsSize / 3);
                int itemsCount = random.nextInt(1, maxItems + 1);
                List<OrderItem> orderItems = generateOrderItems(order, categoryItems.subList(0, itemsCount));
                BigDecimal itemsTotal = order.getItemsTotal();
                if (itemsTotal.compareTo(BigDecimal.ZERO) > 0 && random.nextInt(4) == 0) {
                    // discount either value or percent
                    if (random.nextBoolean()) {
                        int maxDiscount = itemsTotal.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP).intValue();
                        if (maxDiscount > 0) {
                            order.setDiscountValue(BigDecimal.valueOf(random.nextInt(0, maxDiscount)));
                        }
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
            OrderItem item = dataManager.create(OrderItem.class);
            item.setCategoryItem(categoryItem);
            item.setOrder(order);
            item.setQuantity(BigDecimal.valueOf(random.nextInt(2, 10)));
            item.setNetPrice(calculateNetPrice(item));
            item.setVat(calculateVat(item, getDefaultVatPercent()));
            item.setGrossPrice(calculateGrossPrice(item));
            generatedItems.add(item);
        }

        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null) orderItems = new ArrayList<>();
        orderItems.addAll(generatedItems);
        order.setOrderItems(orderItems);
        return generatedItems;
    }

    private BigDecimal getDefaultVatPercent() {
        return crmSettingsService.getDefaultVatPercent();
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
                applyOrderTotalsToInvoice(invoice, order);
                invoice.setStatus(InvoiceStatus.NEW);
                result.add(dataManager.save(invoice));
            }
        }
        return result;
    }

    private void generatePayments(List<Invoice> invoices) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Invoice invoice : invoices) {
            BigDecimal paidTotal = generatePaymentsForInvoice(invoice, random);
            updateInvoiceStatus(invoice, paidTotal);
            dataManager.save(invoice);
        }
    }

    private void applyOrderTotalsToInvoice(Invoice invoice, Order order) {
        calculateInvoiceFieldsFromOrder(order, invoice, getDefaultVatPercent());
    }

    private BigDecimal generatePaymentsForInvoice(Invoice invoice, ThreadLocalRandom random) {
        BigDecimal total = scaleAmount(invoice.getTotal());
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal targetPaid = pickTargetPaid(total, random);
        if (targetPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int paymentsCount = random.nextInt(1, 4); // 1..3
        BigDecimal remaining = targetPaid;
        BigDecimal paid = BigDecimal.ZERO;

        for (int i = 0; i < paymentsCount && remaining.compareTo(BigDecimal.ZERO) > 0; i++) {
            BigDecimal part = (i == paymentsCount - 1)
                    ? remaining
                    : randomPaymentPart(remaining, random);
            part = part.min(remaining);
            if (part.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            Payment payment = dataManager.create(Payment.class);
            payment.setInvoice(invoice);
            LocalDate date = (invoice.getDate() != null ? invoice.getDate() : randomDateWithinYears(2, random))
                    .plusDays(random.nextInt(1, 60));
            payment.setDate(date);
            payment.setAmount(part);
            remaining = remaining.subtract(part);
            paid = paid.add(part);
            dataManager.saveWithoutReload(payment);
        }

        return scaleAmount(paid);
    }

    private BigDecimal pickTargetPaid(BigDecimal total, ThreadLocalRandom random) {
        int roll = random.nextInt(100);
        if (roll < 35) {
            return total;
        }
        if (roll < 75) {
            BigDecimal ratio = BigDecimal.valueOf(0.2 + random.nextDouble(0.7));
            BigDecimal target = total.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            if (target.compareTo(total) >= 0) {
                target = total.subtract(BigDecimal.valueOf(0.01));
            }
            return target.max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal randomPaymentPart(BigDecimal remaining, ThreadLocalRandom random) {
        BigDecimal ratio = BigDecimal.valueOf(0.3 + random.nextDouble(0.5));
        BigDecimal part = remaining.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        if (part.compareTo(BigDecimal.ZERO) <= 0) {
            part = remaining.min(BigDecimal.valueOf(0.01));
        }
        return part;
    }

    private void updateInvoiceStatus(Invoice invoice, BigDecimal paidTotal) {
        BigDecimal total = scaleAmount(invoice.getTotal());
        BigDecimal remaining = total.subtract(scaleAmount(paidTotal));

        if (total.compareTo(BigDecimal.ZERO) == 0 || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            return;
        }

        boolean overdue = invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now());
        if (paidTotal.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(overdue ? InvoiceStatus.OVERDUE : InvoiceStatus.NEW);
        } else {
            invoice.setStatus(overdue ? InvoiceStatus.OVERDUE : InvoiceStatus.PENDING);
        }
    }

    private BigDecimal scaleAmount(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
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

    private OffsetDateTime randomDateInDays(int days, ThreadLocalRandom r) {
        return OffsetDateTime.now()
                .minusDays(r.nextLong(days + 1))
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

    private User adminUser() {
        return dataManager.load(User.class).query("e.username='admin'").one();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @FunctionalInterface
    public interface DemoDataProgressListener {
        void onProgress(String message);
    }
}
