package com.company.crm.app.service.analytics;

import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceRiskAssessmentServiceTest {

    private final InvoiceRiskAssessmentService service = new InvoiceRiskAssessmentService(
            new CategoryAllocationPolicy(),
            new PaymentSettlementPolicy(),
            new ReceivablesAgingPolicy()
    );

    @Test
    void assessInvoiceRisk_twoCategories_partialPayment() {
        // given
        Payment partialPayment = new Payment();
        partialPayment.setAmount(new BigDecimal("500.00"));
        partialPayment.setDate(LocalDate.of(2026, 2, 11));

        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal("1000.00"));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setDate(LocalDate.of(2026, 2, 1));
        invoice.setDueDate(LocalDate.of(2026, 2, 20));
        invoice.setPayments(List.of(partialPayment));

        Category category1 = new Category();
        category1.setCode("CAT1");
        category1.setName("Category 1");

        CategoryItem categoryItem1 = new CategoryItem();
        categoryItem1.setCategory(category1);

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setCategoryItem(categoryItem1);
        orderItem1.setGrossPrice(new BigDecimal("500.00"));
        orderItem1.setNetPrice(new BigDecimal("500.00"));
        orderItem1.setQuantity(BigDecimal.ONE);

        Category category2 = new Category();
        category2.setCode("CAT2");
        category2.setName("Category 2");

        CategoryItem categoryItem2 = new CategoryItem();
        categoryItem2.setCategory(category2);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setCategoryItem(categoryItem2);
        orderItem2.setGrossPrice(new BigDecimal("500.00"));
        orderItem2.setNetPrice(new BigDecimal("500.00"));
        orderItem2.setQuantity(BigDecimal.ONE);

        List<OrderItem> orderItems = List.of(orderItem1, orderItem2);

        // when
        InvoiceRiskAssessmentService.InvoiceRiskAssessmentResult result =
                service.assessInvoiceRisk(invoice, orderItems, LocalDate.of(2026, 2, 21));

        // then
        BigDecimal expectedDistributedPaidPerCategory = new BigDecimal("250.00");
        BigDecimal expectedOpenPerCategory = new BigDecimal("250.00");
        BigDecimal expectedOverdueOpenPerCategory = new BigDecimal("250.00");
        BigDecimal expectedDtcNumeratorPerCategory = new BigDecimal("2500.00");
        BigDecimal expectedDtcDenominatorPerCategory = new BigDecimal("250.00");

        assertThat(result.overpaymentAmount()).isEqualByComparingTo("0.00");
        assertThat(result.categories()).hasSize(2);

        InvoiceRiskAssessmentService.CategoryRiskPosition cat1 = result.categories().stream()
                .filter(c -> c.categoryCode().equals("CAT1"))
                .findFirst()
                .orElseThrow();
        assertThat(cat1.paidAmount()).isEqualByComparingTo(expectedDistributedPaidPerCategory);
        assertThat(cat1.openAmount()).isEqualByComparingTo(expectedOpenPerCategory);
        assertThat(cat1.overdueOpenAmount()).isEqualByComparingTo(expectedOverdueOpenPerCategory);
        assertThat(cat1.dtcNumerator()).isEqualByComparingTo(expectedDtcNumeratorPerCategory);
        assertThat(cat1.dtcDenominator()).isEqualByComparingTo(expectedDtcDenominatorPerCategory);

        InvoiceRiskAssessmentService.CategoryRiskPosition cat2 = result.categories().stream()
                .filter(c -> c.categoryCode().equals("CAT2"))
                .findFirst()
                .orElseThrow();
        assertThat(cat2.paidAmount()).isEqualByComparingTo(expectedDistributedPaidPerCategory);
        assertThat(cat2.openAmount()).isEqualByComparingTo(expectedOpenPerCategory);
        assertThat(cat2.overdueOpenAmount()).isEqualByComparingTo(expectedOverdueOpenPerCategory);
        assertThat(cat2.dtcNumerator()).isEqualByComparingTo(expectedDtcNumeratorPerCategory);
        assertThat(cat2.dtcDenominator()).isEqualByComparingTo(expectedDtcDenominatorPerCategory);
    }

    @Test
    void assessInvoiceRisk_tracksOverpayment() {
        // given
        Payment overPayment = new Payment();
        overPayment.setAmount(new BigDecimal("120.00"));
        overPayment.setDate(LocalDate.of(2026, 2, 2));

        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal("100.00"));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setDate(LocalDate.of(2026, 2, 1));
        invoice.setDueDate(LocalDate.of(2026, 2, 20));
        invoice.setPayments(List.of(overPayment));

        Category category = new Category();
        category.setCode("CAT1");
        category.setName("Category 1");

        CategoryItem categoryItem = new CategoryItem();
        categoryItem.setCategory(category);

        OrderItem orderItem = new OrderItem();
        orderItem.setCategoryItem(categoryItem);
        orderItem.setGrossPrice(new BigDecimal("100.00"));
        orderItem.setNetPrice(new BigDecimal("100.00"));
        orderItem.setQuantity(BigDecimal.ONE);

        List<OrderItem> orderItems = List.of(orderItem);

        // when
        InvoiceRiskAssessmentService.InvoiceRiskAssessmentResult result =
                service.assessInvoiceRisk(invoice, orderItems, LocalDate.of(2026, 2, 21));

        // then
        assertThat(result.overpaymentAmount()).isEqualByComparingTo("20.00");
        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().getFirst().paidAmount()).isEqualByComparingTo("100.00");
        assertThat(result.categories().getFirst().openAmount()).isEqualByComparingTo("0.00");
        assertThat(result.categories().getFirst().overdueOpenAmount()).isEqualByComparingTo("0.00");
    }
}
