package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.report.dataloader.FlagsAndIndicatorsReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlagsAndIndicatorsReportDataLoaderTest extends AbstractTest {

    @Autowired
    private FlagsAndIndicatorsReportDataLoader dataLoader;

    @Test
    void testLoadDataWithValidClient() {
        // Given
        Client client = entities.client("Flags Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> flags = result.get(0);

        // Check that all expected flags are present
        assertThat(flags).containsKeys(
            // Customer Classification Flags
            "isHighValue", "isVIP", "isNew", "isFrequent", "isInactive",

            // Financial Health Indicators
            "hasPaymentIssues", "hasGoodPaymentHistory", "hasOutstandingBalance", "outstandingAmount",

            // Business Relationship Indicators
            "isBusiness", "hasAccountManager",

            // Long-term and activity indicators
            "isLongTerm", "customerTenure", "hasRecentActivity",

            // Sales opportunity and risk assessment
            "hasSalesOpportunity", "isCreditRisk"
        );

        // All boolean flags should be present
        assertThat(flags.get("isHighValue")).isInstanceOf(Boolean.class);
        assertThat(flags.get("isVIP")).isInstanceOf(Boolean.class);
        assertThat(flags.get("isNew")).isInstanceOf(Boolean.class);
        assertThat(flags.get("isFrequent")).isInstanceOf(Boolean.class);
        assertThat(flags.get("isInactive")).isInstanceOf(Boolean.class);
        assertThat(flags.get("hasPaymentIssues")).isInstanceOf(Boolean.class);
        assertThat(flags.get("hasGoodPaymentHistory")).isInstanceOf(Boolean.class);
        assertThat(flags.get("hasOutstandingBalance")).isInstanceOf(Boolean.class);
        assertThat(flags.get("isBusiness")).isInstanceOf(Boolean.class);
        assertThat(flags.get("hasAccountManager")).isInstanceOf(Boolean.class);
        assertThat(flags.get("isLongTerm")).isInstanceOf(Boolean.class);
        assertThat(flags.get("hasRecentActivity")).isInstanceOf(Boolean.class);
        assertThat(flags.get("hasSalesOpportunity")).isInstanceOf(Boolean.class);
        assertThat(flags.get("isCreditRisk")).isInstanceOf(Boolean.class);

        // Special format fields
        assertThat(flags.get("outstandingAmount")).isInstanceOf(String.class);
        assertThat(flags.get("customerTenure")).isNotNull();
    }

    @Test
    void testLoadDataWithNullClient() {
        // Given
        Map<String, Object> params = createParams(null,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> flags = result.get(0);
        assertThat(flags).isEmpty();
    }

    @Test
    void testLoadDataAlwaysReturnsOneRow() {
        // Given
        Client client = entities.client("Single Row Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void testLoadDataUsesDateRangeParameter() {
        // Given
        Client client = entities.client("Date Range Client");
        dataManager.save(client);

        // Different date ranges should potentially affect the flags
        Map<String, Object> params1 = createParams(client,
            LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

        Map<String, Object> params2 = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        // When
        List<Map<String, Object>> result1 = dataLoader.loadData(null, null, params1);
        List<Map<String, Object>> result2 = dataLoader.loadData(null, null, params2);

        // Then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);

        // Both should have the same flag structure
        Map<String, Object> flags1 = result1.get(0);
        Map<String, Object> flags2 = result2.get(0);

        assertThat(flags1.keySet()).isEqualTo(flags2.keySet());
    }

    private Map<String, Object> createParams(Client client, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("client", client);
        params.put("fromDate", java.sql.Date.valueOf(fromDate));
        params.put("toDate", java.sql.Date.valueOf(toDate));
        return params;
    }
}