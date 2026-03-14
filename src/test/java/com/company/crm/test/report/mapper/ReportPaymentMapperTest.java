package com.company.crm.test.report.mapper;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.report.mapper.ReportPaymentMapper;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportPaymentMapperTest extends AbstractTest {

    @Autowired
    private ReportPaymentMapper mapper;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Test
    void testToReportMapWithCompletePayment() {
        // given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order);
        invoice.setNumber("INV-001");

        Payment payment = entities.payment(invoice, LocalDate.of(2024, 1, 20));
        payment.setNumber("PAY-001");
        payment.setAmount(BigDecimal.valueOf(750.50));

        // when
        Map<String, Object> result = mapper.toReportMap(payment);

        // then
        assertThat(result).hasSize(5);
        assertThat(result.get("number")).isEqualTo("PAY-001");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 1, 20));
        assertThat(result.get("dateFormatted")).isEqualTo(datatypeFormatter.formatLocalDate(LocalDate.of(2024, 1, 20)));
        assertThat(result.get("amount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(750.50), datatypeFormatter));
        assertThat(result.get("invoiceNumber")).isEqualTo("INV-001");
    }

    @Test
    void testToReportMapWithNullInvoice() {
        // given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.now(), OrderStatus.NEW);
        Invoice invoice = entities.invoice(client, order);

        Payment payment = entities.payment(invoice, LocalDate.of(2024, 2, 15));
        payment.setNumber("PAY-002");
        payment.setAmount(BigDecimal.valueOf(1000.00));
        payment.setInvoice(null);

        // when
        Map<String, Object> result = mapper.toReportMap(payment);

        // then
        assertThat(result).hasSize(5);
        assertThat(result.get("number")).isEqualTo("PAY-002");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 2, 15));
        assertThat(result.get("dateFormatted")).isEqualTo(datatypeFormatter.formatLocalDate(LocalDate.of(2024, 2, 15)));
        assertThat(result.get("amount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(1000.00), datatypeFormatter));
        assertThat(result.get("invoiceNumber")).isEqualTo("");
    }

    @Test
    void testToReportMapConsistency() {
        // given - Same payment mapped twice
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 3, 1), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order);
        invoice.setNumber("INV-004");

        Payment payment = entities.payment(invoice, LocalDate.of(2024, 4, 10));
        payment.setNumber("PAY-004");
        payment.setAmount(BigDecimal.valueOf(1500.25));

        // when
        Map<String, Object> result1 = mapper.toReportMap(payment);
        Map<String, Object> result2 = mapper.toReportMap(payment);

        // then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.get("number")).isEqualTo("PAY-004");
        assertThat(result1.get("dateFormatted")).isEqualTo(datatypeFormatter.formatLocalDate(LocalDate.of(2024, 4, 10)));
        assertThat(result1.get("amount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(1500.25), datatypeFormatter));
        assertThat(result1.get("invoiceNumber")).isEqualTo("INV-004");
    }

    @Test
    void testToReportMapWithNullAmount_formatsEmptyCurrencyValue() {
        // given
        Client client = entities.client("Null Amount Client");
        Order order = entities.order(client, LocalDate.of(2024, 5, 1), OrderStatus.NEW);
        Invoice invoice = entities.invoice(client, order);
        invoice.setNumber("INV-NULL-AMOUNT");

        Payment payment = entities.payment(invoice, LocalDate.of(2024, 5, 2));
        payment.setNumber("PAY-NULL-AMOUNT");
        payment.setAmount(null);

        // when
        Map<String, Object> result = mapper.toReportMap(payment);

        // then
        assertThat(result.get("amount")).isEqualTo(PriceDataType.defaultFormat(null, datatypeFormatter));
        assertThat(result.get("invoiceNumber")).isEqualTo("INV-NULL-AMOUNT");
    }
}
