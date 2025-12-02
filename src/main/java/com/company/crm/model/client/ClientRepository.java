package com.company.crm.model.client;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends UuidEntityRepository<Client> {
    List<Client> findAllByType(String type);
}