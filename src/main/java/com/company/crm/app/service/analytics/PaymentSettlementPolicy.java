package com.company.crm.app.service.analytics;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain policy: settles incoming payment against category-level exposure.
 */
@Component("crm_PaymentSettlementPolicy")
public class PaymentSettlementPolicy {

    public PaymentSettlement settle(BigDecimal amount, Map<String, BigDecimal> remainingByCategory) {
        LinkedHashMap<String, BigDecimal> distributed = new LinkedHashMap<>();
        BigDecimal totalRemaining = remainingByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentSettlement(distributed, amount);
        }

        BigDecimal toDistribute = amount;
        for (Map.Entry<String, BigDecimal> entry : remainingByCategory.entrySet()) {
            String categoryCode = entry.getKey();
            BigDecimal remaining = entry.getValue();
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal share = remaining.divide(totalRemaining, 8, RoundingMode.HALF_UP);
            BigDecimal allocated = amount.multiply(share).setScale(2, RoundingMode.HALF_UP);
            allocated = allocated.min(remaining);

            if (allocated.compareTo(BigDecimal.ZERO) > 0) {
                distributed.put(categoryCode, allocated);
                toDistribute = toDistribute.subtract(allocated);
            }
        }

        if (toDistribute.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : remainingByCategory.entrySet()) {
                String categoryCode = entry.getKey();
                BigDecimal alreadyDistributed = distributed.getOrDefault(categoryCode, BigDecimal.ZERO);
                BigDecimal remaining = entry.getValue().subtract(alreadyDistributed);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal extra = toDistribute.min(remaining);
                if (extra.compareTo(BigDecimal.ZERO) > 0) {
                    distributed.put(categoryCode, alreadyDistributed.add(extra));
                    toDistribute = toDistribute.subtract(extra);
                }
                if (toDistribute.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }
        }

        return new PaymentSettlement(distributed, toDistribute.max(BigDecimal.ZERO));
    }

    public record PaymentSettlement(
            Map<String, BigDecimal> distributed,
            BigDecimal overpayment
    ) {
    }
}
