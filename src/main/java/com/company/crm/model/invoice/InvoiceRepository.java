package com.company.crm.model.invoice;

import com.company.crm.model.base.UuidEntityRepository;
import com.company.crm.model.client.Client;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends UuidEntityRepository<Invoice> {
    List<Invoice> findAllByClient(Client client, Pageable pageable);
}