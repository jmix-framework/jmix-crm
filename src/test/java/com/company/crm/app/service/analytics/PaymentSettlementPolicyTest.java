package com.company.crm.app.service.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSettlementPolicyTest {

    private final PaymentSettlementPolicy policy = new PaymentSettlementPolicy();

    @Test
    void settle_splitsProportionally() {
        // given
        Map<String, BigDecimal> remaining = new LinkedHashMap<>();
        remaining.put("CAT1", new BigDecimal("500.00"));
        remaining.put("CAT2", new BigDecimal("500.00"));
        BigDecimal incomingPayment = new BigDecimal("500.00");

        // when
        PaymentSettlementPolicy.PaymentSettlement result =
                policy.settle(incomingPayment, remaining);

        // then
        assertThat(result.distributed().get("CAT1")).isEqualByComparingTo("250.00");
        assertThat(result.distributed().get("CAT2")).isEqualByComparingTo("250.00");
        assertThat(result.overpayment()).isEqualByComparingTo("0.00");
        BigDecimal distributedTotal = result.distributed().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(distributedTotal.add(result.overpayment())).isEqualByComparingTo(incomingPayment);
    }

    @Test
    void settle_capsAndReturnsOverpayment() {
        // given
        Map<String, BigDecimal> remaining = new LinkedHashMap<>();
        remaining.put("CAT1", new BigDecimal("100.00"));
        remaining.put("CAT2", new BigDecimal("50.00"));
        BigDecimal incomingPayment = new BigDecimal("300.00");

        // when
        PaymentSettlementPolicy.PaymentSettlement result =
                policy.settle(incomingPayment, remaining);

        // then
        assertThat(result.distributed().get("CAT1")).isEqualByComparingTo("100.00");
        assertThat(result.distributed().get("CAT2")).isEqualByComparingTo("50.00");
        assertThat(result.overpayment()).isEqualByComparingTo("150.00");
        BigDecimal distributedTotal = result.distributed().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(distributedTotal.add(result.overpayment())).isEqualByComparingTo(incomingPayment);
    }

    @Test
    void settle_whenNoRemaining_returnsFullOverpayment() {
        // given
        Map<String, BigDecimal> remaining = new LinkedHashMap<>();
        remaining.put("CAT1", BigDecimal.ZERO);
        BigDecimal incomingPayment = new BigDecimal("10.00");

        // when
        PaymentSettlementPolicy.PaymentSettlement result =
                policy.settle(incomingPayment, remaining);

        // then
        assertThat(result.distributed()).isEmpty();
        assertThat(result.overpayment()).isEqualByComparingTo("10.00");
        BigDecimal distributedTotal = result.distributed().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(distributedTotal.add(result.overpayment())).isEqualByComparingTo(incomingPayment);
    }

    @Test
    void settle_roundingResidual_isAppliedWithoutLosingAmount() {
        // given
        Map<String, BigDecimal> remaining = new LinkedHashMap<>();
        remaining.put("CAT1", new BigDecimal("1.00"));
        remaining.put("CAT2", new BigDecimal("1.00"));
        remaining.put("CAT3", new BigDecimal("1.00"));
        BigDecimal incomingPayment = new BigDecimal("1.00");

        // when
        PaymentSettlementPolicy.PaymentSettlement result = policy.settle(incomingPayment, remaining);

        // then
        assertThat(result.distributed()).hasSize(3);
        assertThat(result.distributed().values())
                .containsExactlyInAnyOrder(
                        new BigDecimal("0.34"),
                        new BigDecimal("0.33"),
                        new BigDecimal("0.33")
                );
        assertThat(result.overpayment()).isEqualByComparingTo("0.00");
        BigDecimal distributedTotal = result.distributed().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(distributedTotal.add(result.overpayment())).isEqualByComparingTo(incomingPayment);
    }
}
