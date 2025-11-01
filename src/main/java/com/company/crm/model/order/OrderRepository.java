package com.company.crm.model.order;

import com.company.crm.model.base.UuidEntityRepository;
import io.jmix.core.repository.JmixDataRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends UuidEntityRepository<Order> {
}