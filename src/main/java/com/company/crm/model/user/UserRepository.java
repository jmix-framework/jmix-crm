package com.company.crm.model.user;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends UuidEntityRepository<User> {
}