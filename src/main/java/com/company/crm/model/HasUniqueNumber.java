package com.company.crm.model;

import com.company.crm.app.service.util.UniqueNumbersService;
import com.company.crm.app.util.context.AppContext;

public interface HasUniqueNumber {
    default String generateNextNumber() {
        return AppContext.getBean(UniqueNumbersService.class).getNextNumber(this.getClass());
    }
}
