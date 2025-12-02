package com.company.crm.model.order;

import com.company.crm.model.base.UuidEntityRepository;
import com.company.crm.model.client.Client;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends UuidEntityRepository<Order> {

}