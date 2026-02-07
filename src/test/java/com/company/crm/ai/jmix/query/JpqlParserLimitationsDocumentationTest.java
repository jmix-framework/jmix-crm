package com.company.crm.ai.jmix.query;

import com.company.crm.AbstractTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("All tests document Jmix-JPQL limitations")
class JpqlParserLimitationsDocumentationTest extends AbstractTest {

    @Test
    void CaseWhenInAggregateFunction_Count() {
        String countCaseWhenQuery = """
            SELECT c.id, c.name,
            COUNT(CASE WHEN o.date >= '2025-01-01' THEN 1 END),
            COUNT(CASE WHEN o.date < '2025-01-01' THEN 1 END)
            FROM Client c LEFT JOIN c.orders o
            GROUP BY c.id, c.name
            """;

        dataManager.loadValues(countCaseWhenQuery).list();
    }


    @Test
    void BetweenMacroInCaseWhen() {
        String betweenMacroInCaseWhenQuery = """
            SELECT c.id, c.name,
            COUNT(CASE WHEN @between(o.date, now-90, now, day) THEN o ELSE NULL END),
            COUNT(CASE WHEN @between(o.date, now-180, now-91, day) THEN o ELSE NULL END)
            FROM Client c LEFT JOIN c.orders o
            GROUP BY c.id, c.name
            """;

        dataManager.loadValues(betweenMacroInCaseWhenQuery).list();
    }

    @Test
    void CaseWhenWithEntityReferences() {
        String caseWhenWithEntityReferenceQuery = """
            SELECT c.id, c.name,
            COUNT(CASE WHEN o.date > '2025-01-01' AND o.total > 1000 THEN o ELSE NULL END)
            FROM Client c LEFT JOIN c.orders o
            GROUP BY c.id, c.name
            """;

        dataManager.loadValues(caseWhenWithEntityReferenceQuery).list();
    }

    @Test
    void NestedAggregatesWithCaseWhen() {
        String avgMaxCaseWhenQuery = """
            SELECT c.id, c.name,
            AVG(CASE WHEN o.total > 1000 THEN o.total END),
            MAX(CASE WHEN @between(o.date, now-90, now, day) THEN o.total ELSE 0 END)
            FROM Client c LEFT JOIN c.orders o
            GROUP BY c.id, c.name
            """;

        dataManager.loadValues(avgMaxCaseWhenQuery).list();
    }

    @Test
    void CaseWhenWithCountExpression() {
        String mixedReturnTypesCaseWhenQuery = """
            SELECT c.id, c.name,
            COUNT(CASE
                WHEN o.total > 1000 THEN 1
                WHEN o.total > 500 THEN o
                ELSE NULL
            END)
            FROM Client c LEFT JOIN c.orders o
            GROUP BY c.id, c.name
            """;

        dataManager.loadValues(mixedReturnTypesCaseWhenQuery).list();
    }

}