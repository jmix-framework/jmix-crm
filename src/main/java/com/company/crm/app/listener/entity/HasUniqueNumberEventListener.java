package com.company.crm.app.listener.entity;

import com.company.crm.app.service.util.UniqueNumbersService;
import com.company.crm.model.HasUniqueNumber;
import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.order.Order;
import io.jmix.core.event.EntitySavingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class HasUniqueNumberEventListener {

    private final UniqueNumbersService uniqueNumbersService;

    public HasUniqueNumberEventListener(UniqueNumbersService uniqueNumbersService) {
        this.uniqueNumbersService = uniqueNumbersService;
    }

    @EventListener
    public void onOrderSaving(final EntitySavingEvent<? extends UuidEntity> event) {
        UuidEntity entity = event.getEntity();

        if (entity instanceof HasUniqueNumber hasUniqueNumber) {
            hasUniqueNumber.applyNumber(uniqueNumbersService.getNextNumber(hasUniqueNumber.getClass()));
        }

        if (entity instanceof Order order) {
            order.setPurchaseOrder(uniqueNumbersService.getNextPurchaseOrderNumber());
        }
    }
}