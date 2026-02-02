package com.company.crm.test.invoice;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.finance.InvoiceService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceServiceTest extends AbstractTest {

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void invoicesCount_filtersByStatusAndDateRange() {
        Client client = entities.client();
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);

        createInvoice(order, client, LocalDate.of(2026, 1, 5), InvoiceStatus.NEW);
        createInvoice(order, client, LocalDate.of(2026, 1, 20), InvoiceStatus.OVERDUE);
        createInvoice(order, client, LocalDate.of(2026, 2, 1), InvoiceStatus.NEW);

        var range = LocalDateRange.from(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(invoiceService.getInvoicesCount(range, InvoiceStatus.NEW)).isEqualTo(1);
        assertThat(invoiceService.getInvoicesCount(range, InvoiceStatus.OVERDUE)).isEqualTo(1);
        assertThat(invoiceService.getAllInvoicesCount(range)).isEqualTo(2);
    }

    @Test
    void overdueInvoices_respectDateRangeAndLimit() {
        Client client = entities.client();
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);

        createInvoice(order, client, LocalDate.of(2026, 1, 5), InvoiceStatus.OVERDUE);
        createInvoice(order, client, LocalDate.of(2026, 1, 15), InvoiceStatus.OVERDUE);
        createInvoice(order, client, LocalDate.of(2026, 1, 25), InvoiceStatus.OVERDUE);
        createInvoice(order, client, LocalDate.of(2026, 2, 1), InvoiceStatus.OVERDUE);

        var range = LocalDateRange.from(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        var overdue = invoiceService.getOverdueInvoices(range, 2);
        assertThat(overdue).hasSize(2);
        assertThat(overdue).allMatch(invoice -> invoice.getStatus() == InvoiceStatus.OVERDUE);
    }

    @Test
    void invoicesCountByStatus_aggregatesValues() {
        Client client = entities.client();
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);

        createInvoice(order, client, LocalDate.of(2026, 1, 5), InvoiceStatus.NEW);
        createInvoice(order, client, LocalDate.of(2026, 1, 6), InvoiceStatus.NEW);
        createInvoice(order, client, LocalDate.of(2026, 1, 7), InvoiceStatus.PAID);

        var result = invoiceService.getInvoicesCountByStatus();

        assertThat(result.get(InvoiceStatus.NEW)).isEqualTo(2);
        assertThat(result.get(InvoiceStatus.PAID)).isEqualTo(1);
    }

    private Invoice createInvoice(Order order, Client client, LocalDate date, InvoiceStatus status) {
        Invoice invoice = entities.invoice(client, order);
        invoice.setDate(date);
        invoice.setStatus(status);
        return saveWithoutReload(invoice);
    }
}
