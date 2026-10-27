package com.company.crm.test.invoice;

import com.company.crm.AbstractTest;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceRepository;
import com.company.crm.model.invoice.InvoiceStatus;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTotalsPersistenceTest extends AbstractTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void dataManagerAndRepositoryPersistTheCalculatedTotal() {
        Invoice invoice = invoice("100", "20", "999");
        invoice = dataManager.save(invoice);
        assertStoredAmounts(invoice, "100", "20", "120");

        invoice.setSubtotal(new BigDecimal("110"));
        invoice.setTotal(new BigDecimal("999"));
        invoice = invoiceRepository.save(invoice);
        assertStoredAmounts(invoice, "110", "20", "130");
    }

    @Test
    void newInvoiceAndClearedAmountsDefaultToZero() {
        Invoice invoice = dataManager.create(Invoice.class);
        invoice.setClient(entities.client());
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("0");
        assertThat(invoice.getVat()).isEqualByComparingTo("0");
        invoice = dataManager.save(invoice);
        assertStoredAmounts(invoice, "0", "0", "0");

        invoice.setSubtotal(null);
        invoice.setVat(null);
        invoice.setTotal(null);
        invoice = dataManager.save(invoice);
        assertStoredAmounts(invoice, "0", "0", "0");
    }

    @Test
    void roundsComponentsBeforeCalculatingTheStoredTotal() {
        Invoice invoice = dataManager.save(invoice("100.005", "20.005", "999"));
        assertStoredAmounts(invoice, "100.01", "20.01", "120.02");
    }

    @Test
    void partialFetchPlanUpdatesPreserveUnchangedFinancialValues() {
        Invoice invoice = dataManager.save(invoice("100", "20", "120"));
        Invoice statusOnly = dataManager.load(Invoice.class).id(invoice.getId())
                .fetchPlan(fp -> fp.add("status")).one();
        statusOnly.setStatus(InvoiceStatus.PENDING);
        assertStoredAmounts(dataManager.save(statusOnly), "100", "20", "120");

        Invoice subtotalOnly = dataManager.load(Invoice.class).id(invoice.getId())
                .fetchPlan(fp -> fp.add("subtotal")).one();
        subtotalOnly.setSubtotal(new BigDecimal("110"));
        assertStoredAmounts(dataManager.save(subtotalOnly), "110", "20", "130");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-0.001"})
    void negativeComponentsCannotBeSaved(String subtotal) {
        Invoice invoice = invoice(subtotal, "20", "19");
        assertThatThrownBy(() -> dataManager.save(invoice)).isInstanceOf(ConstraintViolationException.class);
    }

    private Invoice invoice(String subtotal, String vat, String total) {
        Invoice invoice = dataManager.create(Invoice.class);
        invoice.setClient(entities.client());
        invoice.setSubtotal(new BigDecimal(subtotal));
        invoice.setVat(new BigDecimal(vat));
        invoice.setTotal(new BigDecimal(total));
        return invoice;
    }

    private void assertStoredAmounts(Invoice invoice, String subtotal, String vat, String total) {
        Invoice saved = dataManager.load(Invoice.class).id(invoice.getId()).one();
        assertThat(saved.getSubtotal()).isEqualByComparingTo(subtotal);
        assertThat(saved.getVat()).isEqualByComparingTo(vat);
        assertThat(saved.getTotal()).isEqualByComparingTo(total);
        assertThat(dataManager.loadValue("select i.total from Invoice i where i.id = :id", BigDecimal.class)
                .parameter("id", invoice.getId()).one()).isEqualByComparingTo(total);
    }
}
