package com.company.crm.test.util;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.util.UniqueNumbersService;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.company.crm.app.service.util.UniqueNumbersService.INVOICE_NUMBER_PREFIX;
import static com.company.crm.app.service.util.UniqueNumbersService.ORDER_NUMBER_PREFIX;
import static com.company.crm.app.service.util.UniqueNumbersService.PAYMENT_NUMBER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

class UniqueNumbersServiceTest extends AbstractTest {

    @Autowired
    private UniqueNumbersService uniqueNumbersService;

    @Test
    void nextNumber_usesPrefixAndIncrements() {
        String first = uniqueNumbersService.getNextOrderNumber();
        String second = uniqueNumbersService.getNextOrderNumber();

        assertThat(first).startsWith(ORDER_NUMBER_PREFIX);
        assertThat(second).startsWith(ORDER_NUMBER_PREFIX);
        assertThat(extractNumber(second)).isGreaterThan(extractNumber(first));
    }

    @Test
    void getNextNumber_supportsKnownEntities() {
        String order = uniqueNumbersService.getNextNumber(Order.class);
        String invoice = uniqueNumbersService.getNextNumber(Invoice.class);
        String payment = uniqueNumbersService.getNextNumber(Payment.class);

        assertThat(order).startsWith(ORDER_NUMBER_PREFIX);
        assertThat(invoice).startsWith(INVOICE_NUMBER_PREFIX);
        assertThat(payment).startsWith(PAYMENT_NUMBER_PREFIX);
    }

    private long extractNumber(String value) {
        return Long.parseLong(value.substring(value.indexOf('-') + 1));
    }
}
