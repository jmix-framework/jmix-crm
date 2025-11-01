package com.company.crm.model.client;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends UuidEntityRepository<Client> {
}