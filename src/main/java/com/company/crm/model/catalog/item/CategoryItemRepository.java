package com.company.crm.model.catalog.item;

import com.company.crm.model.base.UuidEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryItemRepository extends UuidEntityRepository<CategoryItem> {
    Optional<CategoryItem> findByCode(String code);
}