package com.company.crm.model.user.activity.client;

import com.company.crm.model.client.Client;
import com.company.crm.model.user.activity.UserActivity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@JmixEntity
@Table(name = "CLIENT_USER_ACITIVTY", indexes = {
        @Index(name = "IDX_CLIENT_USER_ACITIVTY_UER", columnList = "USER_ID"),
        @Index(name = "IDX_CLIENT_USER_ACITIVTY_CLIENT", columnList = "CLIENT_ID")
})
@Entity
public class ClientUserActivity extends UserActivity {

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

}