package com.company.crm.app.service.finance;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.payment.PaymentRepository;
import io.jmix.data.Sequence;
import io.jmix.data.Sequences;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Sequence PAYMENT_NUMBER_SEQUENCE =
            Sequence.withName("CRM_INVOICE_NUMBER").setStartValue(1000);

    private final PaymentRepository paymentRepository;
    private final Sequences sequences;

    public PaymentService(PaymentRepository paymentRepository, Sequences sequences) {
        this.paymentRepository = paymentRepository;
        this.sequences = sequences;
    }

    public String getNextPaymentNumber() {
        return "PAY-" + sequences.createNextValue(PAYMENT_NUMBER_SEQUENCE);
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

    public Map<Client, BigDecimal> getPaymentsTotalsByClients(int limit) {
        return paymentRepository.fluentValuesLoader("select e.invoice.client as client, sum(e.amount) as total " +
                        "from Payment e " +
                        "group by e.invoice.client " +
                        "order by total desc")
                .properties("client", "total")
                .maxResults(limit)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getValue("client"),
                        r -> r.getValue("total")
                ));
    }

    public List<Payment> getBiggestPayments(int limit) {
        return paymentRepository.queryLoader("select e from Payment e order by e.amount desc")
                .maxResults(limit)
                .list();
    }
}
