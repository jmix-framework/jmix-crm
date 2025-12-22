package com.company.crm.model.order;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends UuidEntityRepository<Order> {

}