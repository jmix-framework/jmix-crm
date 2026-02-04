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

    public static final String PURCHASE_ORDER_NUMBER_PREFIX = "PO-";
    public static final String ORDER_NUMBER_PREFIX = "ORD-";
    public static final String INVOICE_NUMBER_PREFIX = "INV-";
    public static final String PAYMENT_NUMBER_PREFIX = "PAY-";

    private static final Sequence PAYMENT_NUMBER_SEQUENCE =
            defaultNumberSequence("CRM_PAYMENT_NUMBER");

    private static final Sequence INVOICE_NUMBER_SEQUENCE =
            defaultNumberSequence("CRM_INVOICE_NUMBER");

    private static final Sequence ORDER_NUMBER_SEQUENCE =
            defaultNumberSequence("CRM_ORDER_NUMBER");

    private static final Sequence PURCHASE_ORDER_NUMBER_SEQUENCE =
            defaultNumberSequence("CRM_PURCHASE_ORDER_NUMBER");

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
        return ORDER_NUMBER_PREFIX + sequences.createNextValue(ORDER_NUMBER_SEQUENCE);
    }

    private String getNextPaymentNumber() {
        return PAYMENT_NUMBER_PREFIX + sequences.createNextValue(PAYMENT_NUMBER_SEQUENCE);
    }

    public String getNextInvoiceNumber() {
        return INVOICE_NUMBER_PREFIX + sequences.createNextValue(INVOICE_NUMBER_SEQUENCE);
    }

    public String getNextPurchaseOrderNumber() {
        return PURCHASE_ORDER_NUMBER_PREFIX + sequences.createNextValue(PURCHASE_ORDER_NUMBER_SEQUENCE);
    }

    private static Sequence defaultNumberSequence(String sequenceName) {
        return Sequence.withName(sequenceName).setStartValue(1000);
    }
}
