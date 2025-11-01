package com.company.crm.model.payment;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends UuidEntityRepository<Payment> {
}