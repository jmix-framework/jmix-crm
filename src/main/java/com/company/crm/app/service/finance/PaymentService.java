package com.company.crm.app.service.finance;

import com.company.crm.model.payment.Payment;
import com.company.crm.model.payment.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> loadPayments(LocalDate startDate, LocalDate endDate) {
        return paymentRepository.listByQuery("e.date >= ?1 and e.date <= ?2", startDate, endDate);
    }

    /**
     * Retrieves the total sum of all payment amounts.
     *
     * @return the total sum of payment amounts as a {@code BigDecimal}.
     */
    public BigDecimal getPaymentsTotalSum() {
        return paymentRepository.fluentValueLoader(
                        "select sum(e.amount) as total " +
                                "from Payment e " +
                                "order by total desc", BigDecimal.class)
                .optional().orElse(BigDecimal.ZERO);
    }
}
