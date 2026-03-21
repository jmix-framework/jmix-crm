package com.company.crm.test.init;

import com.company.crm.AbstractTest;
import com.company.crm.app.util.init.DemoDataGenerator;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.model.user.task.UserTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataGeneratorTest extends AbstractTest {

    @Autowired
    DemoDataGenerator demoDataGenerator;

    @Override
    protected boolean cleanDataAfterEach() {
        return false;
    }

    @Test
    void loadsDemoDataFromCsvFiles() {
        systemAuthenticator.runWithSystem(() -> demoDataGenerator.initDemoDataIfNeeded());

        assertThat(dataManager.load(Client.class).all().list()).hasSize(30);
        assertThat(dataManager.load(Contact.class).all().list()).hasSizeGreaterThan(0);
        assertThat(dataManager.load(Order.class).all().list()).hasSizeGreaterThan(0);
        assertThat(dataManager.load(Invoice.class).all().list()).hasSizeGreaterThan(0);
        assertThat(dataManager.load(Payment.class).all().list()).hasSizeGreaterThan(0);
        assertThat(dataManager.load(UserTask.class).all().list()).hasSizeGreaterThan(0);
        assertThat(dataManager.load(ClientUserActivity.class).all().list()).hasSizeGreaterThan(0);
    }
}
