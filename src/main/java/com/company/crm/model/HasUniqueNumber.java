package com.company.crm.model;

import com.company.crm.app.service.util.UniqueNumbersService;
import com.company.crm.app.util.context.AppContext;
import io.jmix.core.Messages;

public interface HasUniqueNumber {
    default String generateNextNumber() {
        return AppContext.getBean(UniqueNumbersService.class).getNextNumber(this.getClass());
    }

    default String getNumberWillBeGeneratedMessage() {
        return AppContext.getBean(Messages.class).getMessage("numberWillBeGenerated");
    }
}
