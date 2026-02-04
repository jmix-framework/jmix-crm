package com.company.crm.app.util.ui.datacontext;

import io.jmix.core.querycondition.Condition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;

public final class DataContextUtils {

    public static JmixDataRepositoryContext addCondition(JmixDataRepositoryContext context, Condition condition) {
        Condition resultCondition;
        if (context.condition() != null) {
            resultCondition = LogicalCondition.and(context.condition(), condition);
        } else {
            resultCondition = condition;
        }
        return new JmixDataRepositoryContext(context.fetchPlan(), resultCondition, context.hints());
    }

    private DataContextUtils() {
    }
}
