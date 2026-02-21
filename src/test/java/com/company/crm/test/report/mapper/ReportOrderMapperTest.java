package com.company.crm.test.report.mapper;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.mapper.ReportOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportOrderMapperTest extends AbstractTest {

    @Autowired
    private ReportOrderMapper mapper;

    @Test
    void testToReportMapWithCompleteOrder() {
        // Given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 15), OrderStatus.DONE);
        order.setNumber("ORD-001");
        order.setTotal(BigDecimal.valueOf(1250.75));
        order.setComment("Complete order with all fields");

        // When
        Map<String, Object> result = mapper.toReportMap(order);

        // Then
        assertThat(result).hasSize(6);
        assertThat(result.get("number")).isEqualTo("ORD-001");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.get("dateFormatted")).isInstanceOf(String.class);
        assertThat(result.get("status")).isInstanceOf(String.class);
        assertThat(result.get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(1250.75)));
        assertThat(result.get("comment")).isEqualTo("Complete order with all fields");
    }

    @Test
    void testToReportMapWithEmptyStrings() {
        // Given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 2, 1), OrderStatus.NEW);
        order.setNumber("");
        order.setTotal(BigDecimal.ZERO);
        order.setComment("");

        // When
        Map<String, Object> result = mapper.toReportMap(order);

        // Then
        assertThat(result).hasSize(6);
        assertThat(result.get("number")).isEqualTo("");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(result.get("dateFormatted")).isInstanceOf(String.class);
        assertThat(result.get("status")).isInstanceOf(String.class);
        assertThat(result.get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.ZERO));
        assertThat(result.get("comment")).isEqualTo("");
    }

    @Test
    void testToReportMapConsistency() {
        // Given - Same order mapped twice
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 3, 10), OrderStatus.DONE);
        order.setNumber("ORD-002");
        order.setTotal(BigDecimal.valueOf(500.00));
        order.setComment("Test consistency");

        // When
        Map<String, Object> result1 = mapper.toReportMap(order);
        Map<String, Object> result2 = mapper.toReportMap(order);

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.get("number")).isEqualTo("ORD-002");
        assertThat(result1.get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(500.00)));
    }
}