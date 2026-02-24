package com.company.crm.app.service.analytics;

import com.company.crm.AbstractServiceTest;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashflowAnalyticsServiceIntegrationTest extends AbstractServiceTest<CashflowAnalyticsService> {

    @Test
    void testSingleInvoice_twoCategories_partialPayment() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Category cat1 = entities.category("Cat 1", "CAT1");
            Category cat2 = entities.category("Cat 2", "CAT2");
            
            CategoryItem item1 = entities.categoryItem("Item 1", "I1", cat1, BigDecimal.valueOf(100), UomType.PIECES);
            CategoryItem item2 = entities.categoryItem("Item 2", "I2", cat2, BigDecimal.valueOf(100), UomType.PIECES);
            
            Client client = entities.client("Test Client");
            
            // Order with 2 items from different categories (500 each)
            Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);
            var oi1 = entities.orderItem(order, item1, BigDecimal.valueOf(5)); // 500
            var oi2 = entities.orderItem(order, item2, BigDecimal.valueOf(5)); // 500
            order.setOrderItems(List.of(oi1, oi2));
            order.setTotal(BigDecimal.valueOf(1000));
            order = dataManager.save(order);
            
            // Invoice for the order
            Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1000), InvoiceStatus.PENDING, LocalDate.now());
            
            // Partial payment of 500 (should be split 250/250)
            entities.payment(invoice, LocalDate.now().plusDays(10), BigDecimal.valueOf(500));
            BigDecimal expectedInvoicedPerCategory = new BigDecimal("500.00");
            BigDecimal expectedPaidPerCategory = new BigDecimal("250.00");
            BigDecimal expectedOpenPerCategory = new BigDecimal("250.00");
            double expectedWeightedDtc = 10.0; // (250*10)/250

            // when
            List<CategoryRiskMetrics> results = service.assessCategoryCashflowRisk(null, null, client.getId(), null);
            
            // then
            assertThat(results).hasSize(2);
            
            CategoryRiskMetrics m1 = results.stream().filter(r -> r.categoryCode().equals("CAT1")).findFirst().get();
            assertThat(m1.invoicedAmount()).isEqualByComparingTo(expectedInvoicedPerCategory);
            assertThat(m1.paidAmount()).isEqualByComparingTo(expectedPaidPerCategory);
            assertThat(m1.openAmount()).isEqualByComparingTo(expectedOpenPerCategory);
            assertThat(m1.dtcDaysWeighted()).isEqualTo(expectedWeightedDtc);
            
            CategoryRiskMetrics m2 = results.stream().filter(r -> r.categoryCode().equals("CAT2")).findFirst().get();
            assertThat(m2.invoicedAmount()).isEqualByComparingTo(expectedInvoicedPerCategory);
            assertThat(m2.paidAmount()).isEqualByComparingTo(expectedPaidPerCategory);
            assertThat(m2.openAmount()).isEqualByComparingTo(expectedOpenPerCategory);
            assertThat(m2.dtcDaysWeighted()).isEqualTo(expectedWeightedDtc);
        });
    }

    @Test
    void testSingleInvoice_twoPayments_weightedDtc() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Category cat1 = entities.category("Cat 1", "CAT1");
            CategoryItem item1 = entities.categoryItem("Item 1", "I1", cat1, BigDecimal.valueOf(100), UomType.PIECES);
            Client client = entities.client("Test Client");
            
            Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);
            var oi1 = entities.orderItem(order, item1, BigDecimal.valueOf(10)); // 1000
            order.setOrderItems(List.of(oi1));
            order.setTotal(BigDecimal.valueOf(1000));
            order = dataManager.save(order);
            
            Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1000), InvoiceStatus.PENDING, LocalDate.now());
            
            // P1: 300 after 5 days, P2: 700 after 25 days
            // Weighted DTC = (300*5 + 700*25) / 1000 = (1500 + 17500) / 1000 = 19000 / 1000 = 19.0
            entities.payment(invoice, LocalDate.now().plusDays(5), BigDecimal.valueOf(300));
            entities.payment(invoice, LocalDate.now().plusDays(25), BigDecimal.valueOf(700));
            BigDecimal expectedInvoiced = new BigDecimal("1000.00");
            BigDecimal expectedPaid = new BigDecimal("1000.00");
            BigDecimal expectedOpen = new BigDecimal("0.00");
            double expectedWeightedDtc = 19.0;
            
            // when
            List<CategoryRiskMetrics> results = service.assessCategoryCashflowRisk(null, null, client.getId(), null);
            
            // then
            assertThat(results).hasSize(1);
            CategoryRiskMetrics m = results.get(0);
            assertThat(m.invoicedAmount()).isEqualByComparingTo(expectedInvoiced);
            assertThat(m.paidAmount()).isEqualByComparingTo(expectedPaid);
            assertThat(m.openAmount()).isEqualByComparingTo(expectedOpen);
            assertThat(m.dtcDaysWeighted()).isEqualTo(expectedWeightedDtc);
        });
    }

    @Test
    void testOverdueRar_byDueDateFallback() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Category cat1 = entities.category("Risk Cat", "RISK");
            CategoryItem item1 = entities.categoryItem("Item 1", "I1", cat1, BigDecimal.valueOf(1000), UomType.PIECES);
            Client client = entities.client("Risk Client");
            
            Order order = entities.order(client, LocalDate.now().minusDays(40), OrderStatus.DONE);
            var oi1 = entities.orderItem(order, item1, BigDecimal.valueOf(1));
            order.setOrderItems(List.of(oi1));
            order.setTotal(BigDecimal.valueOf(1000));
            order = dataManager.save(order);
            
            // Invoice created 40 days ago, due 30 days ago
            Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1000), InvoiceStatus.PENDING, LocalDate.now().minusDays(40));
            invoice.setDueDate(LocalDate.now().minusDays(30));
            saveWithoutReload(invoice);
            
            // when
            List<CategoryRiskMetrics> results = service.assessCategoryCashflowRisk(null, null, client.getId(), LocalDate.now());
            
            // then
            assertThat(results).hasSize(1);
            CategoryRiskMetrics m = results.get(0);
            assertThat(m.overdueOpenAmount()).isEqualByComparingTo("1000.00");
        });
    }

    @Test
    void testIncludePaidFalse_excludesPaidInvoices() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Category category = entities.category("Filter Cat", "FILTER");
            CategoryItem item = entities.categoryItem("Filter Item", "FI", category, BigDecimal.valueOf(100), UomType.PIECES);
            Client client = entities.client("Filter Client");

            Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(100));
            order = dataManager.save(order);

            Invoice paidInvoice = entities.invoice(client, order, BigDecimal.valueOf(500), InvoiceStatus.PAID, LocalDate.now().minusDays(10));
            entities.payment(paidInvoice, LocalDate.now().minusDays(5), BigDecimal.valueOf(500));

            Invoice pendingInvoice = entities.invoice(client, order, BigDecimal.valueOf(300), InvoiceStatus.PENDING, LocalDate.now().minusDays(8));
            entities.payment(pendingInvoice, LocalDate.now().minusDays(4), BigDecimal.valueOf(100));

            // when
            CategoryCashflowRiskAssessmentResult withPaid = service.assessCategoryCashflowRiskReport(
                    null, null, client.getId(), true, LocalDate.now());
            CategoryCashflowRiskAssessmentResult withoutPaid = service.assessCategoryCashflowRiskReport(
                    null, null, client.getId(), false, LocalDate.now());

            // then
            assertThat(withPaid.riskByCategory()).hasSize(1);
            CategoryRiskMetrics withPaidMetric = withPaid.riskByCategory().getFirst();
            assertThat(withPaidMetric.invoicedAmount()).isEqualByComparingTo("800.00");
            assertThat(withPaidMetric.paidAmount()).isEqualByComparingTo("600.00");
            assertThat(withPaidMetric.openAmount()).isEqualByComparingTo("200.00");

            assertThat(withoutPaid.riskByCategory()).hasSize(1);
            CategoryRiskMetrics filteredMetric = withoutPaid.riskByCategory().getFirst();
            assertThat(filteredMetric.invoicedAmount()).isEqualByComparingTo("300.00");
            assertThat(filteredMetric.paidAmount()).isEqualByComparingTo("100.00");
            assertThat(filteredMetric.openAmount()).isEqualByComparingTo("200.00");
            assertThat(filteredMetric.invoicedAmount()).isNotEqualByComparingTo(withPaidMetric.invoicedAmount());
            assertThat(filteredMetric.paidAmount()).isNotEqualByComparingTo(withPaidMetric.paidAmount());
        });
    }

    @Test
    void testOverdueOpenAmounts_areReflectedInRiskByCategory() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Category category = entities.category("Critical Cat", "CRIT");
            CategoryItem item = entities.categoryItem("Critical Item", "CI", category, BigDecimal.valueOf(1000), UomType.PIECES);
            Client client = entities.client("Critical Client");

            Order order = entities.order(client, LocalDate.now().minusDays(45), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(1000));
            order = dataManager.save(order);

            Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1000), InvoiceStatus.PENDING, LocalDate.now().minusDays(40));
            invoice.setDueDate(LocalDate.now().minusDays(30));
            saveWithoutReload(invoice);
            entities.payment(invoice, LocalDate.now().minusDays(20), BigDecimal.valueOf(200));

            // when
            CategoryCashflowRiskAssessmentResult result = service.assessCategoryCashflowRiskReport(
                    null, null, client.getId(), true, LocalDate.now());

            // then
            assertThat(result.riskByCategory()).hasSize(1);
            CategoryRiskMetrics metric = result.riskByCategory().getFirst();
            assertThat(metric.categoryCode()).isEqualTo("CRIT");
            assertThat(metric.invoicedAmount()).isEqualByComparingTo("1000.00");
            assertThat(metric.paidAmount()).isEqualByComparingTo("200.00");
            assertThat(metric.openAmount()).isEqualByComparingTo("800.00");
            assertThat(metric.overdueOpenAmount()).isEqualByComparingTo("800.00");
        });
    }
}
