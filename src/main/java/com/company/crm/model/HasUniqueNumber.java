package com.company.crm.model;

import com.company.crm.app.util.context.AppContext;
import io.jmix.core.Messages;

public interface HasUniqueNumber {

    void applyNumber(String number);

    default String getNumberWillBeGeneratedMessage() {
        return AppContext.getBean(Messages.class).getMessage("numberWillBeGenerated");
    }
}
