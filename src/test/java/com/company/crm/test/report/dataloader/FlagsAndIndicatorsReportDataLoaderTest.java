package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.report.dataloader.FlagsAndIndicatorsReportDataLoader;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlagsAndIndicatorsReportDataLoaderTest extends AbstractTest {

    @Autowired
    private FlagsAndIndicatorsReportDataLoader dataLoader;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Test
    void testLoadDataWithValidClient() {
        // given
        Client client = entities.client("Flags Client", 400, ClientType.INDIVIDUAL);

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
        Map<String, Object> flags = result.get(0);


        assertThat(flags.get("isHighValue")).isEqualTo(false);
        assertThat(flags.get("isVIP")).isEqualTo(false);
        assertThat(flags.get("isNew")).isEqualTo(false);
        assertThat(flags.get("isFrequent")).isEqualTo(false);
        assertThat(flags.get("isInactive")).isEqualTo(true);
        assertThat(flags.get("hasPaymentIssues")).isEqualTo(false);
        assertThat(flags.get("hasGoodPaymentHistory")).isEqualTo(false);
        assertThat(flags.get("hasOutstandingBalance")).isEqualTo(false);
        assertThat(flags.get("isBusiness")).isEqualTo(false);
        assertThat(flags.get("hasAccountManager")).isEqualTo(false);
        assertThat(flags.get("isLongTerm")).isEqualTo(true);
        assertThat(flags.get("hasRecentActivity")).isEqualTo(false);
        assertThat(flags.get("hasSalesOpportunity")).isEqualTo(false);
        assertThat(flags.get("isCreditRisk")).isEqualTo(false);
        assertThat(flags.get("outstandingAmount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.ZERO, datatypeFormatter));
        assertThat((String) flags.get("customerTenure")).contains("year");
    }

    @Test
    void testLoadDataWithNullClient() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("client", null);
        params.put("fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)));
        params.put("toDate", Date.valueOf(LocalDate.of(2024, 1, 31)));

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
        Map<String, Object> flags = result.get(0);
        assertThat(flags).isEmpty();
    }

    @Test
    void testLoadDataAlwaysReturnsOneRow() {
        // given
        Client client = entities.client("Single Row Client");

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    void testLoadDataUsesDateRangeParameter() {
        // given
        Client client = entities.client("Date Range Client");

        // Different date ranges should potentially affect the flags
        Map<String, Object> params1 = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2023, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2023, 12, 31))
        );

        Map<String, Object> params2 = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 12, 31))
        );

        // when
        List<Map<String, Object>> result1 = dataLoader.loadData(null, null, params1);
        List<Map<String, Object>> result2 = dataLoader.loadData(null, null, params2);

        // then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);

        // Both should have the same flag structure
        Map<String, Object> flags1 = result1.get(0);
        Map<String, Object> flags2 = result2.get(0);

        assertThat(flags1.keySet()).isEqualTo(flags2.keySet());
    }

}
