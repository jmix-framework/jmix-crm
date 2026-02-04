package com.company.crm.util.extenstion;

import ch.qos.logback.classic.Level;
import com.company.crm.app.util.log.LoggerUtils;
import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.client.Client;
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
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import javax.sql.DataSource;

import static org.springframework.test.jdbc.JdbcTestUtils.deleteFromTables;

public class DataCleaner implements AfterAllCallback, AfterEachCallback {

    private static final List<Class<? extends UuidEntity>> ENTITIES_REMOVING_ORDER = List.of(
            ClientUserActivity.class,
            UserProfileUserActivity.class,
            Payment.class,
            Invoice.class,
            OrderItem.class,
            Order.class,
            Client.class,
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
    public void afterEach(ExtensionContext context) throws Exception {
        var testOpt = context.getTestInstance();
        if (testOpt.isEmpty()) {
            return;
        }

        var test = testOpt.get();
        Method cleanDataMethod = ReflectionUtils.getRequiredMethod(test.getClass(), "cleanDataAfterEach");
        cleanDataMethod.trySetAccessible();
        Boolean needToClean = (Boolean) cleanDataMethod.invoke(test);

        if (needToClean) {
            cleanData(context);
        }
    }

    private void cleanData(ExtensionContext context) {
        log.info("Removing test data...");
        var dataSource = ExtensionUtils.getBean(context, DataSource.class);
        String[] tablesToClean = getTablesToClean(context);
        LoggerUtils.runWithLevel(JdbcTestUtils.class, Level.WARN, () ->
                deleteFromTables(new JdbcTemplate(dataSource), tablesToClean));
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
