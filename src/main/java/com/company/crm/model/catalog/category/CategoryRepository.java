package com.company.crm.model.catalog.category;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends UuidEntityRepository<Category> {
    Optional<Category> findByCode(String code);

    boolean existsByCode(String code);
}