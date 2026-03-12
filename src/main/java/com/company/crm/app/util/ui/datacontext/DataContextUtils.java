package com.company.crm.app.util.ui.datacontext;

import io.jmix.core.Sort;
import io.jmix.core.querycondition.Condition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.model.BaseCollectionLoader;

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

    public static void installSortByCreatedDate(BaseCollectionLoader dataLoader) {
        dataLoader.setSort(Sort.by(Sort.Direction.DESC, "createdDate"));
    }

    private DataContextUtils() {
    }
}
