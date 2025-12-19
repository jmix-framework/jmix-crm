package com.company.crm.app.service.finance;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.payment.PaymentRepository;
import io.jmix.core.FluentValueLoader;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> loadPayments(LocalDateRange dateRange) {
        return paymentRepository.listByQuery("e.date >= ?1 and e.date <= ?2",
                dateRange.startDate(), dateRange.endDate());
    }

    /**
     * Retrieves the total sum of all payment amounts.
     *
     * @return the total sum of payment amounts as a {@code BigDecimal}.
     */
    public BigDecimal getPaymentsTotalSum(Order... order) {
        var queryBuilder = new StringBuilder()
                .append("select sum(e.amount) as total ")
                .append("from Payment e ");

        if (order.length > 0) {
            queryBuilder.append("where e.invoice.order in :orders ");
        }

        queryBuilder.append("order by total desc");

        var loader = paymentRepository.fluentValueLoader(queryBuilder.toString(), BigDecimal.class);

        if (order.length > 0) {
            loader.parameter("orders", Arrays.stream(order).toList());
        }

        return loader.optional().orElse(BigDecimal.ZERO);
    }
}
