package com.company.crm.view.payment;

import com.company.crm.model.payment.Payment;
import com.company.crm.model.payment.PaymentRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

@Route(value = "payments/:id", layout = MainView.class)
@ViewController(id = "Payment.detail")
@ViewDescriptor(path = "payment-detail-view.xml")
@EditedEntityContainer("paymentDc")
public class PaymentDetailView extends StandardDetailView<Payment> {

    @Autowired
    private PaymentRepository paymentRepository;

    @Install(to = "paymentDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Payment> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return paymentRepository.findById(id, fetchPlan);
    }
}