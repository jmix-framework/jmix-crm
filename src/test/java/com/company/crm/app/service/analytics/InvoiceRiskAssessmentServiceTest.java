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
        Invoice invoice = invoice("1000.00", InvoiceStatus.PENDING, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20),
                List.of(payment("500.00", LocalDate.of(2026, 2, 11))));

        List<OrderItem> orderItems = List.of(
                orderItem("CAT1", "Category 1", "500.00"),
                orderItem("CAT2", "Category 2", "500.00")
        );

        InvoiceRiskAssessmentService.InvoiceRiskAssessmentResult result =
                service.assessInvoiceRisk(invoice, orderItems, LocalDate.of(2026, 2, 21));

        assertThat(result.overpaymentAmount()).isEqualByComparingTo("0.00");
        assertThat(result.categories()).hasSize(2);

        InvoiceRiskAssessmentService.CategoryRiskPosition cat1 = result.categories().stream()
                .filter(c -> c.categoryCode().equals("CAT1"))
                .findFirst()
                .orElseThrow();
        assertThat(cat1.paidAmount()).isEqualByComparingTo("250.00");
        assertThat(cat1.openAmount()).isEqualByComparingTo("250.00");
        assertThat(cat1.dtcNumerator()).isEqualByComparingTo("2500.00");
        assertThat(cat1.dtcDenominator()).isEqualByComparingTo("250.00");
        assertThat(result.criticalInvoices()).hasSize(2);
        assertThat(result.criticalInvoices().getFirst().daysOverdue()).isEqualTo(1L);
    }

    @Test
    void assessInvoiceRisk_tracksOverpayment() {
        Invoice invoice = invoice("100.00", InvoiceStatus.PENDING, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20),
                List.of(payment("120.00", LocalDate.of(2026, 2, 2))));

        List<OrderItem> orderItems = List.of(orderItem("CAT1", "Category 1", "100.00"));

        InvoiceRiskAssessmentService.InvoiceRiskAssessmentResult result =
                service.assessInvoiceRisk(invoice, orderItems, LocalDate.of(2026, 2, 21));

        assertThat(result.overpaymentAmount()).isEqualByComparingTo("20.00");
        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().getFirst().paidAmount()).isEqualByComparingTo("100.00");
        assertThat(result.categories().getFirst().openAmount()).isEqualByComparingTo("0.00");
        assertThat(result.criticalInvoices()).isEmpty();
    }

    private Invoice invoice(String total, InvoiceStatus status, LocalDate invoiceDate, LocalDate dueDate, List<Payment> payments) {
        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal(total));
        invoice.setStatus(status);
        invoice.setDate(invoiceDate);
        invoice.setDueDate(dueDate);
        invoice.setPayments(payments);
        return invoice;
    }

    private Payment payment(String amount, LocalDate date) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal(amount));
        payment.setDate(date);
        return payment;
    }

    private OrderItem orderItem(String categoryCode, String categoryName, String total) {
        Category category = new Category();
        category.setCode(categoryCode);
        category.setName(categoryName);

        CategoryItem categoryItem = new CategoryItem();
        categoryItem.setCategory(category);

        OrderItem orderItem = new OrderItem();
        orderItem.setCategoryItem(categoryItem);
        orderItem.setGrossPrice(new BigDecimal(total));
        orderItem.setNetPrice(new BigDecimal(total));
        orderItem.setQuantity(BigDecimal.ONE);
        return orderItem;
    }
}
