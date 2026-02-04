package com.company.crm.test.home;

import com.company.crm.AbstractUiTest;
import com.company.crm.view.home.HomeView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HomeViewTest extends AbstractUiTest {

    @Test
    void opensHomeView() {
        var view = viewTestSupport.navigateTo(HomeView.class);
        assertThat(view).isInstanceOf(HomeView.class);
    }
}
