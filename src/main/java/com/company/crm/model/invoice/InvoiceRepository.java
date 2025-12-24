package com.company.crm.model.invoice;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends UuidEntityRepository<Invoice> {
}