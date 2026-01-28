package com.company.crm.app.service.util;

import com.company.crm.model.HasUniqueNumber;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import io.jmix.data.Sequence;
import io.jmix.data.Sequences;
import org.springframework.stereotype.Service;

@Service
public class UniqueNumbersService {

    private static final Sequence PAYMENT_NUMBER_SEQUENCE =
            Sequence.withName("CRM_PAYMENT_NUMBER").setStartValue(1000);

    private static final Sequence INVOICE_NUMBER_SEQUENCE =
            Sequence.withName("CRM_INVOICE_NUMBER").setStartValue(1000);

    private static final Sequence ORDER_NUMBER_SEQUENCE =
            Sequence.withName("CRM_ORDER_NUMBER").setStartValue(1000);

    private static final Sequence PURCHASE_ORDER_NUMBER_SEQUENCE =
            Sequence.withName("CRM_PURCHASE_ORDER_NUMBER").setStartValue(1000);

    private final Sequences sequences;

    public UniqueNumbersService(Sequences sequences) {
        this.sequences = sequences;
    }

    public String getNextNumber(Class<? extends HasUniqueNumber> clazz) {
        if (Payment.class.equals(clazz)) {
            return getNextPaymentNumber();
        } else if (Invoice.class.equals(clazz)) {
            return getNextInvoiceNumber();
        } else if (Order.class.equals(clazz)) {
            return getNextOrderNumber();
        }
        throw new IllegalArgumentException("Unsupported class: " + clazz.getName());
    }

    public String getNextOrderNumber() {
        return "ORD-" + sequences.createNextValue(ORDER_NUMBER_SEQUENCE);
    }

    private String getNextPaymentNumber() {
        return "PAY-" + sequences.createNextValue(PAYMENT_NUMBER_SEQUENCE);
    }

    public String getNextInvoiceNumber() {
        return "INV-" + sequences.createNextValue(INVOICE_NUMBER_SEQUENCE);
    }

    public String getNextPurchaseOrderNumber() {
        return "POR-" + sequences.createNextValue(PURCHASE_ORDER_NUMBER_SEQUENCE);
    }
}
