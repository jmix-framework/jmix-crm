package com.company.crm.test.payment;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.payment.Payment;
import com.company.crm.view.payment.PaymentDetailView;
import com.company.crm.view.payment.PaymentListView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentViewsTest extends AbstractUiTest {

    @Test
    void opensPaymentListView() {
        var view = viewTestSupport.navigateTo(PaymentListView.class);
        assertThat(view).isInstanceOf(PaymentListView.class);
    }

    @Test
    void opensPaymentDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(Payment.class, PaymentDetailView.class);
        assertThat(view).isInstanceOf(PaymentDetailView.class);
    }
}
