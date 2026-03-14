package com.company.crm.test.report.mapper;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.mapper.ReportOrderMapper;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportOrderMapperTest extends AbstractTest {

    @Autowired
    private ReportOrderMapper mapper;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Test
    void testToReportMapWithCompleteOrder() {
        // given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 15), OrderStatus.DONE);
        order.setNumber("ORD-001");
        order.setTotal(BigDecimal.valueOf(1250.75));
        order.setComment("Complete order with all fields");

        // when
        Map<String, Object> result = mapper.toReportMap(order);

        // then
        assertThat(result).hasSize(6);
        assertThat(result.get("number")).isEqualTo("ORD-001");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.get("dateFormatted")).isInstanceOf(String.class);
        assertThat((String) result.get("dateFormatted")).isNotBlank();
        assertThat(result.get("status")).isInstanceOf(String.class);
        assertThat((String) result.get("status")).isNotBlank();
        assertThat(result.get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(1250.75), datatypeFormatter));
        assertThat(result.get("comment")).isEqualTo("Complete order with all fields");
    }

    @Test
    void testToReportMapWithEmptyStrings() {
        // given
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 2, 1), OrderStatus.NEW);
        order.setNumber("");
        order.setTotal(BigDecimal.ZERO);
        order.setComment("");

        // when
        Map<String, Object> result = mapper.toReportMap(order);

        // then
        assertThat(result).hasSize(6);
        assertThat(result.get("number")).isEqualTo("");
        assertThat(result.get("date")).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(result.get("dateFormatted")).isInstanceOf(String.class);
        assertThat((String) result.get("dateFormatted")).isNotBlank();
        assertThat(result.get("status")).isInstanceOf(String.class);
        assertThat((String) result.get("status")).isNotBlank();
        assertThat(result.get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.ZERO, datatypeFormatter));
        assertThat(result.get("comment")).isEqualTo("");
    }

    @Test
    void testToReportMapConsistency() {
        // given - Same order mapped twice
        Client client = entities.client("Test Client");
        Order order = entities.order(client, LocalDate.of(2024, 3, 10), OrderStatus.DONE);
        order.setNumber("ORD-002");
        order.setTotal(BigDecimal.valueOf(500.00));
        order.setComment("Test consistency");

        // when
        Map<String, Object> result1 = mapper.toReportMap(order);
        Map<String, Object> result2 = mapper.toReportMap(order);

        // then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.get("number")).isEqualTo("ORD-002");
        assertThat(result1.get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(500.00), datatypeFormatter));
    }

    @Test
    void testToReportMapWithNullNumberAndComment_usesEntityDefaults() {
        // given
        Client client = entities.client("Null Fields Client");
        Order order = entities.order(client, LocalDate.of(2024, 4, 1), OrderStatus.NEW);
        order.setNumber(null);
        order.setTotal(BigDecimal.valueOf(100));
        order.setComment(null);

        // when
        Map<String, Object> result = mapper.toReportMap(order);

        // then
        assertThat(result.get("number")).isEqualTo("Will be generated");
        assertThat(result.get("comment")).isEqualTo("");
        assertThat(result.get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(100), datatypeFormatter));
    }
}
