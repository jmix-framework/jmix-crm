package com.company.crm.model.user.activity;

import com.company.crm.model.base.UuidEntityRepository;
import com.company.crm.model.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.OffsetDateTime;
import java.util.List;

@NoRepositoryBean
public interface UserActivityRepository <T extends UserActivity> extends UuidEntityRepository<T> {

    List<T> findAllByUserOrderByCreatedDateDesc(User user, Pageable pageable);
    List<T> findAllByUserAndCreatedDateGreaterThanEqualAndCreatedDateLessThanEqualOrderByCreatedDateDesc(User user, OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable);
    List<T> findAllByCreatedDateGreaterThanEqualAndCreatedDateLessThanEqualOrderByCreatedDateDesc(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}