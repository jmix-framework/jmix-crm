package com.company.crm.model.contact;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends UuidEntityRepository<Contact> {
}