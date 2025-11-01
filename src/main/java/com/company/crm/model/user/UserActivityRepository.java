package com.company.crm.model.user;

import com.company.crm.model.base.UuidEntityRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

public interface UserActivityRepository extends UuidEntityRepository<UserActivity> {
    List<UserActivity> findAllByUser(User user, Pageable pageable);
    @Query("select u from UserActivity u where u.createdDate between ?1 and ?2")
    List<UserActivity> findAllByCreatedDateBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}