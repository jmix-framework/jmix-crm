package com.company.crm.test.client;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.client.Client;
import com.company.crm.view.client.ClientDetailView;
import com.company.crm.view.client.ClientListView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientViewsTest extends AbstractUiTest {

    @Test
    void opensClientListView() {
        var view = viewTestSupport.navigateTo(ClientListView.class);
        assertThat(view).isInstanceOf(ClientListView.class);
    }

    @Test
    void opensClientDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(Client.class, ClientDetailView.class);
        assertThat(view).isInstanceOf(ClientDetailView.class);
    }
}
