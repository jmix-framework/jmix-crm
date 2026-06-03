package com.company.crm.util.extenstion;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageEntityReference;
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
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.model.user.activity.userprofile.UserProfileUserActivity;
import com.company.crm.model.user.task.UserTask;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.sql.DataSource;

import static org.springframework.test.jdbc.JdbcTestUtils.deleteFromTables;

public class DataCleaner implements AfterAllCallback, AfterEachCallback {

    private static final List<Class<? extends UuidEntity>> ENTITIES_REMOVING_ORDER = List.of(
            ChatMessageEntityReference.class,
            AiConversationAttachment.class,
            ChatMessage.class,
            AiConversation.class,
            ClientUserActivity.class,
            UserProfileUserActivity.class,
            Payment.class,
            Invoice.class,
            OrderItem.class,
            Order.class,
            Contact.class,
            Client.class,
            CategoryItemComment.class,
            CategoryItem.class,
            Category.class,
            UserTask.class
    );

    private static final List<Class<? extends UuidEntity>> EXCLUDED_ENTITIES = List.of(
            User.class
    );
    private static final Logger log = LoggerFactory.getLogger(DataCleaner.class);

    @Override
    public void afterAll(ExtensionContext context) {
        cleanData(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var testOpt = context.getTestInstance();
        if (testOpt.isEmpty()) {
            return;
        }

        var test = testOpt.get();
        boolean needToClean = resolveNeedToClean(test);

        if (needToClean) {
            cleanData(context);
        }
    }

    private boolean resolveNeedToClean(Object testInstance) {
        Object currentInstance = testInstance;

        while (currentInstance != null) {
            Method cleanDataMethod = findCleanDataAfterEachMethod(currentInstance.getClass());
            if (cleanDataMethod != null) {
                try {
                    cleanDataMethod.trySetAccessible();
                    Object result = cleanDataMethod.invoke(currentInstance);
                    return Boolean.TRUE.equals(result);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Failed to invoke cleanDataAfterEach() on " + currentInstance.getClass().getName(), e);
                }
            }

            currentInstance = getEnclosingInstance(currentInstance);
        }

        throw new IllegalStateException(
                "Could not resolve cleanDataAfterEach() on test instance " + testInstance.getClass().getName()
                        + " or any enclosing test instance.");
    }

    private Method findCleanDataAfterEachMethod(Class<?> type) {
        if (type == null) {
            return null;
        }

        try {
            return type.getDeclaredMethod("cleanDataAfterEach");
        } catch (NoSuchMethodException ignored) {
            return findCleanDataAfterEachMethod(type.getSuperclass());
        }
    }

    private Object getEnclosingInstance(Object instance) {
        try {
            Field outerField = instance.getClass().getDeclaredField("this$0");
            outerField.trySetAccessible();
            return outerField.get(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void cleanData(ExtensionContext context) {
        log.info("Removing test data...");
        var dataSource = ExtensionUtils.getBean(context, DataSource.class);
        String[] tablesToClean = getTablesToClean(context);

        deleteFromTables(new JdbcTemplate(dataSource), tablesToClean);
        log.info("Test data has been removed");
    }

    private String[] getTablesToClean(ExtensionContext context) {
        var metadataTools = ExtensionUtils.getBean(context, MetadataTools.class);
        return metadataTools.getAllJpaEntityMetaClasses().stream()
                .filter(metaClass -> {
                    Class<?> clazz = metaClass.getJavaClass();
                    if (EXCLUDED_ENTITIES.contains(clazz)) {
                        return false;
                    } else if (User.class.isAssignableFrom(clazz)) {
                        return false;
                    } else {
                        return UuidEntity.class.isAssignableFrom(clazz);
                    }
                })
                .sorted((metaClass1, metaClass2) -> {
                    int indexOf1 = getRemovingIndex(metaClass1);
                    int indexOf2 = getRemovingIndex(metaClass2);
                    return indexOf1 - indexOf2;
                })
                .map(metadataTools::getDatabaseTable)
                .toArray(String[]::new);
    }

    private int getRemovingIndex(MetaClass metaClass) {
        int index = ENTITIES_REMOVING_ORDER.indexOf(metaClass.getJavaClass());
        if (index != -1) {
            return index;
        }

        for (int i = 0; i < ENTITIES_REMOVING_ORDER.size(); i++) {
            if (ENTITIES_REMOVING_ORDER.get(i).isAssignableFrom(metaClass.getJavaClass())) {
                return i;
            }
        }

        return Integer.MAX_VALUE;
    }
}
