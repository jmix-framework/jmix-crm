package com.company.crm.model.client;

import com.company.crm.model.base.UuidEntityRepository;
import io.jmix.core.FetchPlan;
import io.jmix.core.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends UuidEntityRepository<Client> {

    @Query("select c from Client c " +
            "where c.orders is not empty")
    List<Client> findAllByOrdersIsNotEmpty(FetchPlan fetchPlan);

    @Query("select c from Client c " +
            "where c.invoices is not empty")
    List<Client> findAllWithInvoices(FetchPlan fetchPlan);

    @Query("select distinct c from Client c " +
            "join c.invoices i " +
            "join i.payments p")
    List<Client> findAllWithPayments(FetchPlan fetchPlan);

    List<Client> findAllByAccountManagerNotNull();
}