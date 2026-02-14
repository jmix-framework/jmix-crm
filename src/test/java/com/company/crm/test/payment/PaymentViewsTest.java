package com.company.crm.test.payment;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.payment.Payment;
import com.company.crm.util.ViewTestSupport;
import com.company.crm.util.extenstion.AuthenticatedAs;
import com.company.crm.view.payment.PaymentDetailView;
import com.company.crm.view.payment.PaymentListView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@AuthenticatedAs(AuthenticatedAs.ADMIN_USERNAME)
class PaymentViewsTest extends AbstractUiTest {

    @Autowired
    private ViewTestSupport viewTestSupport;

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
