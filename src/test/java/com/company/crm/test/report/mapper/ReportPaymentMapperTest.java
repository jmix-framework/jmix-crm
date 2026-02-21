package com.company.crm.test.report.mapper;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.report.mapper.ReportPaymentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportPaymentMapperTest extends AbstractTest {

    @Autowired
    private ReportPaymentMapper mapper;

    @Test
    void testToReportMapWithCompletePayment() {
        // Given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order);
        invoice.setNumber("INV-001");

        Payment payment = entities.payment(invoice, LocalDate.of(2024, 1, 20));
        payment.setNumber("PAY-001");
        payment.setAmount(BigDecimal.valueOf(750.50));

        // When
        Map<String, Object> result = mapper.toReportMap(payment);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result.get("number")).isEqualTo("PAY-001");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 1, 20));
        assertThat(result.get("dateFormatted")).isInstanceOf(String.class);
        assertThat(result.get("amount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(750.50)));
        assertThat(result.get("invoiceNumber")).isEqualTo("INV-001");
    }

    @Test
    void testToReportMapWithNullInvoice() {
        // Given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.now(), OrderStatus.NEW);
        Invoice invoice = entities.invoice(client, order);

        Payment payment = entities.payment(invoice, LocalDate.of(2024, 2, 15));
        payment.setNumber("PAY-002");
        payment.setAmount(BigDecimal.valueOf(1000.00));
        payment.setInvoice(null);

        // When
        Map<String, Object> result = mapper.toReportMap(payment);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result.get("number")).isEqualTo("PAY-002");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 2, 15));
        assertThat(result.get("dateFormatted")).isInstanceOf(String.class);
        assertThat(result.get("amount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(1000.00)));
        assertThat(result.get("invoiceNumber")).isEqualTo("");
    }

    @Test
    void testToReportMapConsistency() {
        // Given - Same payment mapped twice
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 3, 1), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order);
        invoice.setNumber("INV-004");

        Payment payment = entities.payment(invoice, LocalDate.of(2024, 4, 10));
        payment.setNumber("PAY-004");
        payment.setAmount(BigDecimal.valueOf(1500.25));

        // When
        Map<String, Object> result1 = mapper.toReportMap(payment);
        Map<String, Object> result2 = mapper.toReportMap(payment);

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.get("number")).isEqualTo("PAY-004");
        assertThat(result1.get("amount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(1500.25)));
        assertThat(result1.get("invoiceNumber")).isEqualTo("INV-004");
    }
}