package com.company.crm.model.invoice;

import com.company.crm.model.base.UuidEntityRepository;
import io.jmix.core.repository.JmixDataRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InvoiceRepository extends UuidEntityRepository<Invoice> {
}