package com.company.crm.test.invoice;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.view.invoice.InvoiceDetailView;
import com.company.crm.view.invoice.InvoiceListView;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceViewsTest extends AbstractUiTest {

    @Test
    void opensInvoiceListView() {
        var view = viewTestSupport.navigateTo(InvoiceListView.class);
        assertThat(view).isInstanceOf(InvoiceListView.class);
    }

    @Test
    void opensInvoiceDetailView() {
        Client client = entities.client();
        Order order = entities.order(client, LocalDate.now(), OrderStatus.NEW);
        Invoice invoice = entities.invoice(client, order);
        invoice.setStatus(InvoiceStatus.NEW);
        invoice.setDate(LocalDate.now());
        invoice.setSubtotal(java.math.BigDecimal.ZERO);
        saveWithoutReload(invoice);

        var view = viewTestSupport.navigateToDetailView(Invoice.class, invoice, InvoiceDetailView.class);
        assertThat(view).isInstanceOf(InvoiceDetailView.class);
    }
}
