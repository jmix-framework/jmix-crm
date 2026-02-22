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
        Map<String, BigDecimal> remaining = new LinkedHashMap<>();
        remaining.put("CAT1", new BigDecimal("500.00"));
        remaining.put("CAT2", new BigDecimal("500.00"));

        PaymentSettlementPolicy.PaymentSettlement result =
                policy.settle(new BigDecimal("500.00"), remaining);

        assertThat(result.distributed().get("CAT1")).isEqualByComparingTo("250.00");
        assertThat(result.distributed().get("CAT2")).isEqualByComparingTo("250.00");
        assertThat(result.overpayment()).isEqualByComparingTo("0.00");
    }

    @Test
    void settle_capsAndReturnsOverpayment() {
        Map<String, BigDecimal> remaining = new LinkedHashMap<>();
        remaining.put("CAT1", new BigDecimal("100.00"));
        remaining.put("CAT2", new BigDecimal("50.00"));

        PaymentSettlementPolicy.PaymentSettlement result =
                policy.settle(new BigDecimal("300.00"), remaining);

        assertThat(result.distributed().get("CAT1")).isEqualByComparingTo("100.00");
        assertThat(result.distributed().get("CAT2")).isEqualByComparingTo("50.00");
        assertThat(result.overpayment()).isEqualByComparingTo("150.00");
    }

    @Test
    void settle_whenNoRemaining_returnsFullOverpayment() {
        Map<String, BigDecimal> remaining = new LinkedHashMap<>();
        remaining.put("CAT1", BigDecimal.ZERO);

        PaymentSettlementPolicy.PaymentSettlement result =
                policy.settle(new BigDecimal("10.00"), remaining);

        assertThat(result.distributed()).isEmpty();
        assertThat(result.overpayment()).isEqualByComparingTo("10.00");
    }
}
