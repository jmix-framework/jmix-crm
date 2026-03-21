package com.company.crm.app.util.init;

import com.company.crm.app.config.SpringProfiles;
import com.company.crm.app.service.catalog.CatalogImportSettings;
import com.company.crm.app.service.catalog.CatalogService;
import com.company.crm.model.address.Address;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
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
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SystemAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates deterministic demo data from CSV files under /demo-data/.
 * Dates are stored as day-offsets relative to "today" so they stay fresh on each import.
 * If clients table is not empty, does nothing.
 */
@Component
public class DemoDataGenerator implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(DemoDataGenerator.class);

    private static final DemoDataProgressListener NO_OP_PROGRESS = message -> {
    };

    private final Messages messages;
    private final Environment environment;
    private final SpringProfiles springProfiles;
    private final CatalogService catalogService;
    private final UnconstrainedDataManager dataManager;
    private final SystemAuthenticator systemAuthenticator;
    private final CurrentAuthentication currentAuthentication;
    private final DynamicAttributesInitializer dynamicAttributesInitializer;

    public DemoDataGenerator(UnconstrainedDataManager dataManager,
                             Environment environment,
                             CatalogService catalogService, SystemAuthenticator systemAuthenticator,
                             CurrentAuthentication currentAuthentication, SpringProfiles springProfiles,
                             Messages messages, DynamicAttributesInitializer dynamicAttributesInitializer) {
        this.environment = environment;
        this.dataManager = dataManager;
        this.catalogService = catalogService;
        this.systemAuthenticator = systemAuthenticator;
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
        log.info("Initializing demo data from CSV files...");
        publishProgress(progressListener, "Starting demo data generation");

        publishProgress(progressListener, messages.getMessage("demoData.progress.createDynamicAttributes"));
        dynamicAttributesInitializer.createDynamicAttributesIfNeeded();

        publishProgress(progressListener, messages.getMessage("demoData.progress.configuring"));
        List<User> users = loadDemoUsers();
        Map<String, User> usersByUsername = new HashMap<>();
        for (User u : users) {
            usersByUsername.put(u.getUsername(), u);
        }

        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingTasks"));
        loadUserTasks(usersByUsername);

        publishProgress(progressListener, messages.getMessage("demoData.progress.importingCatalog"));
        Map<Category, List<CategoryItem>> catalog = generateCatalog();

        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingClients"));
        Map<UUID, Client> clientsById = loadClients(usersByUsername);

        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingContacts"));
        loadContacts(clientsById);

        publishProgress(progressListener, messages.getMessage("demoData.progress.generatingOrders"));
        Map<String, Order> ordersByNumber = loadOrders(clientsById, catalog);

        publishProgress(progressListener, messages.getMessage("demoData.progress.generatingInvoices"));
        Map<String, Invoice> invoicesByNumber = loadInvoices(clientsById, ordersByNumber);

        publishProgress(progressListener, messages.getMessage("demoData.progress.generatingPayments"));
        loadPayments(invoicesByNumber);

        publishProgress(progressListener, messages.getMessage("demoData.progress.creatingActivities"));
        loadClientActivities(clientsById, usersByUsername);

        publishProgress(progressListener, messages.getMessage("demoData.progress.finalizing"));
        log.info("Demo data initialization finished: " +
                        "categories={}, categoriesItems={}, " +
                        "clients={} contacts={} orders={} " +
                        "invoices={} payments={}",
                catalog.size(),
                catalog.values().stream().mapToLong(Collection::size).sum(),
                clientsById.size(),
                dataManager.loadValue("select count(c) from Contact c", Long.class).one(),
                dataManager.loadValue("select count(o) from Order_ o", Long.class).one(),
                dataManager.loadValue("select count(i) from Invoice i", Long.class).one(),
                dataManager.loadValue("select count(p) from Payment p", Long.class).one()
        );
    }

    // ---- Users (created via Liquibase in 010-init-user.xml) ----

    private List<User> loadDemoUsers() {
        log.info("Loading demo users (alice, bob) from database...");
        User alice = dataManager.load(User.class).query("e.username = :u").parameter("u", "alice").one();
        User bob = dataManager.load(User.class).query("e.username = :u").parameter("u", "robert").one();
        return List.of(alice, bob);
    }

    // ---- Catalog (from xlsx, same as before) ----

    private Map<Category, List<CategoryItem>> generateCatalog() {
        log.info("Generating catalog from catalog.xlsx...");
        try (InputStream inputStream = getClass().getResourceAsStream("/demo-data/catalog.xlsx")) {
            if (inputStream == null) {
                log.error("catalog.xlsx not found in classpath!");
                return Map.of();
            }
            return catalogService.updateCatalog(new CatalogImportSettings(inputStream));
        } catch (IOException e) {
            log.error("Failed to load catalog.xlsx", e);
            return Map.of();
        }
    }

    // ---- Clients from CSV ----

    private Map<UUID, Client> loadClients(Map<String, User> usersByUsername) {
        log.info("Loading clients from CSV...");
        Map<UUID, Client> result = new HashMap<>();
        for (String[] row : readCsv("/demo-data/clients.csv")) {
            Client client = dataManager.create(Client.class);
            UUID id = UUID.fromString(row[0]);
            client.setName(row[1]);
            client.setFullName(row[2]);
            client.setType(ClientType.valueOf(row[3]));
            client.setVatNumber(row[4]);
            client.setRegNumber(row[5]);
            client.setWebsite(row[6]);

            Address address = dataManager.create(Address.class);
            address.setPostalCode(row[7]);
            address.setCountry(row[8]);
            address.setCity(row[9]);
            address.setBuilding(row[10]);
            address.setStreet(row[11]);
            address.setApartment(row[12]);
            client.setAddress(address);

            if (row.length > 13 && !row[13].isEmpty()) {
                User manager = usersByUsername.get(row[13]);
                if (manager != null) {
                    client.setAccountManager(manager);
                }
            }

            Client saved = dataManager.save(client);
            result.put(id, saved);
        }
        log.info("Loaded {} clients", result.size());
        return result;
    }

    // ---- Contacts from CSV ----

    private void loadContacts(Map<UUID, Client> clientsById) {
        log.info("Loading contacts from CSV...");
        List<Contact> toSave = new ArrayList<>();
        for (String[] row : readCsv("/demo-data/contacts.csv")) {
            Client client = clientsById.get(UUID.fromString(row[0]));
            if (client == null) continue;

            Contact contact = dataManager.create(Contact.class);
            contact.setClient(client);
            contact.setPerson(row[1]);
            contact.setPosition(row[2]);
            contact.setPhone(row[3]);
            contact.setEmail(row[4]);
            if (!row[5].isEmpty()) {
                contact.setStartDate(LocalDate.now().plusDays(Long.parseLong(row[5])));
            }
            if (!row[6].isEmpty()) {
                contact.setEndDate(LocalDate.now().plusDays(Long.parseLong(row[6])));
            }
            toSave.add(contact);
        }
        dataManager.saveWithoutReload(toSave.toArray());
        log.info("Loaded {} contacts", toSave.size());
    }

    // ---- User Tasks from CSV ----

    private void loadUserTasks(Map<String, User> usersByUsername) {
        log.info("Loading user tasks from CSV...");
        for (String[] row : readCsv("/demo-data/user-tasks.csv")) {
            User author = usersByUsername.get(row[0]);
            if (author == null) continue;

            UserTask task = dataManager.create(UserTask.class);
            task.setAuthor(author);
            task.setTitle(row[1]);
            task.setDescription(row[2]);
            if (!row[3].isEmpty()) {
                task.setDueDate(LocalDate.now().plusDays(Long.parseLong(row[3])));
            }
            task.setIsCompleted(Boolean.parseBoolean(row[4]));
            dataManager.saveWithoutReload(task);
        }
    }

    // ---- Orders from CSV ----

    private Map<String, Order> loadOrders(Map<UUID, Client> clientsById, Map<Category, List<CategoryItem>> catalog) {
        log.info("Loading orders from CSV...");

        Map<String, CategoryItem> categoryItemsByCode = new HashMap<>();
        catalog.values().stream().flatMap(Collection::stream)
                .forEach(item -> categoryItemsByCode.put(item.getCode(), item));

        Map<String, Order> ordersByNumber = new HashMap<>();
        List<String[]> orderRows = readCsv("/demo-data/orders.csv");
        List<String[]> orderItemRows = readCsv("/demo-data/order-items.csv");

        SaveContext saveContext = new SaveContext().setDiscardSaved(true);

        for (String[] row : orderRows) {
            String number = row[0];
            Client client = clientsById.get(UUID.fromString(row[1]));
            if (client == null) continue;

            Order order = dataManager.create(Order.class);
            order.setClient(client);
            if (!row[2].isEmpty()) {
                order.setDate(LocalDate.now().plusDays(Long.parseLong(row[2])));
            }
            order.setStatus(OrderStatus.valueOf(row[3]));
            order.setTotal(new BigDecimal(row[4]));
            if (!row[5].isEmpty() && !"0".equals(row[5])) {
                order.setDiscountValue(new BigDecimal(row[5]));
            }
            if (!row[6].isEmpty() && !"0".equals(row[6])) {
                order.setDiscountPercent(new BigDecimal(row[6]));
            }
            if (row.length > 7 && !row[7].isEmpty()) {
                order.setComment(row[7]);
            }

            List<OrderItem> items = new ArrayList<>();
            for (String[] itemRow : orderItemRows) {
                if (!itemRow[0].equals(number)) continue;
                CategoryItem ci = categoryItemsByCode.get(itemRow[1]);
                if (ci == null) continue;

                OrderItem item = dataManager.create(OrderItem.class);
                item.setOrder(order);
                item.setCategoryItem(ci);
                item.setQuantity(new BigDecimal(itemRow[2]));
                item.setNetPrice(new BigDecimal(itemRow[3]));
                item.setVat(new BigDecimal(itemRow[4]));
                item.setGrossPrice(new BigDecimal(itemRow[5]));
                items.add(item);
            }
            order.setOrderItems(items);

            saveContext.saving(order);
            for (OrderItem item : items) {
                saveContext.saving(item);
            }
            ordersByNumber.put(number, order);
        }
        dataManager.save(saveContext);
        log.info("Loaded {} orders", ordersByNumber.size());
        return ordersByNumber;
    }

    // ---- Invoices from CSV ----

    private Map<String, Invoice> loadInvoices(Map<UUID, Client> clientsById, Map<String, Order> ordersByNumber) {
        log.info("Loading invoices from CSV...");
        Map<String, Invoice> invoicesByNumber = new HashMap<>();

        for (String[] row : readCsv("/demo-data/invoices.csv")) {
            String number = row[0];
            Client client = clientsById.get(UUID.fromString(row[1]));
            if (client == null) continue;

            Invoice invoice = dataManager.create(Invoice.class);
            invoice.setClient(client);
            Order order = ordersByNumber.get(row[2]);
            if (order != null) {
                invoice.setOrder(order);
            }
            if (!row[3].isEmpty()) {
                invoice.setDate(LocalDate.now().plusDays(Long.parseLong(row[3])));
            }
            if (!row[4].isEmpty()) {
                invoice.setDueDate(LocalDate.now().plusDays(Long.parseLong(row[4])));
            }
            invoice.setSubtotal(new BigDecimal(row[5]));
            invoice.setVat(new BigDecimal(row[6]));
            invoice.setTotal(new BigDecimal(row[7]));
            invoice.setStatus(InvoiceStatus.valueOf(row[8]));

            Invoice saved = dataManager.save(invoice);
            invoicesByNumber.put(number, saved);
        }
        log.info("Loaded {} invoices", invoicesByNumber.size());
        return invoicesByNumber;
    }

    // ---- Payments from CSV ----

    private void loadPayments(Map<String, Invoice> invoicesByNumber) {
        log.info("Loading payments from CSV...");
        int count = 0;
        for (String[] row : readCsv("/demo-data/payments.csv")) {
            Invoice invoice = invoicesByNumber.get(row[1]);
            if (invoice == null) continue;

            Payment payment = dataManager.create(Payment.class);
            payment.setInvoice(invoice);
            if (!row[2].isEmpty()) {
                payment.setDate(LocalDate.now().plusDays(Long.parseLong(row[2])));
            }
            payment.setAmount(new BigDecimal(row[3]));
            dataManager.saveWithoutReload(payment);
            count++;
        }
        log.info("Loaded {} payments", count);
    }

    // ---- Client Activities from CSV ----

    private void loadClientActivities(Map<UUID, Client> clientsById, Map<String, User> usersByUsername) {
        log.info("Loading client activities from CSV...");
        for (String[] row : readCsv("/demo-data/client-activities.csv")) {
            Client client = clientsById.get(UUID.fromString(row[0]));
            User user = usersByUsername.get(row[1]);
            if (client == null || user == null) continue;

            ClientUserActivity activity = dataManager.create(ClientUserActivity.class);
            activity.setClient(client);
            activity.setUser(user);
            activity.setActionDescription(row[2]);
            dataManager.saveWithoutReload(activity);

            if (!row[3].isEmpty()) {
                OffsetDateTime createdDate = OffsetDateTime.now().plusDays(Long.parseLong(row[3]));
                activity.setCreatedDate(createdDate);
                dataManager.saveWithoutReload(activity);
            }
        }
    }

    // ---- CSV Reader ----

    private List<String[]> readCsv(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("CSV file not found: {}", resourcePath);
                return List.of();
            }
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                csvReader.skip(1); // skip header
                return csvReader.readAll();
            }
        } catch (IOException | CsvException e) {
            log.error("Failed to read CSV: {}", resourcePath, e);
            return List.of();
        }
    }

    // ---- Infrastructure ----

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

    private void publishProgress(DemoDataProgressListener progressListener, String message) {
        try {
            progressListener.onProgress(message);
        } catch (Exception e) {
            log.debug("Ignoring demo data progress update failure", e);
        }
    }

    @FunctionalInterface
    public interface DemoDataProgressListener {
        void onProgress(String message);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
