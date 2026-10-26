package com.company.crm.test.init;

import com.company.crm.AbstractTest;
import com.company.crm.app.util.init.DemoDataGenerator;
import com.company.crm.app.util.price.PriceCalculator;
import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.model.user.task.UserTask;
import io.jmix.core.FetchPlan;
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
        var orders = dataManager.load(Order.class).all()
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                        .add("orderItems", FetchPlan.BASE)
                        .add("invoices", FetchPlan.BASE))
                .list();
        assertThat(orders).hasSize(187);
        for (Order order : orders) {
            assertThat(order.getTotal()).as("Total of %s", order.getNumber())
                    .isEqualByComparingTo(PriceCalculator.calculateTotal(order))
                    .isNotNegative();
            assertThat(order.getSubTotal().add(order.getVat()))
                    .isEqualByComparingTo(order.getTotal());
            assertThat(order.getInvoiced()).isLessThanOrEqualTo(order.getTotal());
        }

        var invoices = dataManager.load(Invoice.class).all()
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                        .add("order", order -> order.addFetchPlan(FetchPlan.BASE)
                                .add("orderItems", FetchPlan.BASE))
                        .add("payments", FetchPlan.BASE))
                .list();
        assertThat(invoices).hasSize(142);
        for (Invoice invoice : invoices) {
            assertThat(invoice.getSubtotal()).as("Subtotal of %s", invoice.getNumber())
                    .isEqualByComparingTo(invoice.getOrder().getSubTotal());
            assertThat(invoice.getVat()).isEqualByComparingTo(invoice.getOrder().getVat());
            assertThat(invoice.getTotal()).isEqualByComparingTo(invoice.getOrder().getTotal())
                    .isEqualByComparingTo(PriceCalculator.calculateTotal(invoice));
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                assertThat(invoice.getPaymentsSum()).as("Payments for %s", invoice.getNumber())
                        .isEqualByComparingTo(invoice.getTotal());
            } else {
                assertThat(invoice.getPaymentsSum()).as("Payments for %s", invoice.getNumber())
                        .isLessThan(invoice.getTotal());
            }
        }
        assertThat(dataManager.load(Payment.class).all().list()).hasSize(189);
        assertThat(dataManager.load(UserTask.class).all().list()).hasSizeGreaterThan(0);
        assertThat(dataManager.load(ClientUserActivity.class).all().list()).hasSizeGreaterThan(0);
    }
}
