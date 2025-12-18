package com.company.crm.model.user.activity.client;

import com.company.crm.model.client.Client;
import com.company.crm.model.user.User;
import com.company.crm.model.user.activity.UserActivityRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

public interface ClientUserActivityRepository extends UserActivityRepository<ClientUserActivity> {

    List<ClientUserActivity> findAllByClient(Client client, Pageable pageable);

    List<ClientUserActivity> findAllByClientAndUser(Client client, User user, Pageable pageable);

    @Query("select u from ClientUserActivity u where u.createdDate between ?1 and ?2")
    List<ClientUserActivity> findAllByCreatedDateBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    @Query("select u from ClientUserActivity u where u.user = ?1 and u.createdDate between ?2 and ?3")
    List<ClientUserActivity> findAllByUserAndCreatedDateBetween(User user, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    @Query("select u from ClientUserActivity u where u.client = ?1 and u.createdDate between ?2 and ?3")
    List<ClientUserActivity> findAllByClientAndCreatedDateBetween(Client client, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    @Query("select u from ClientUserActivity u where u.client = ?1 and u.user = ?2 and u.createdDate between ?3 and ?4")
    List<ClientUserActivity> findAllByClientAndUserAndCreatedDateBetween(Client client, User user, OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}