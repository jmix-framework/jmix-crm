package com.company.crm.test.main;

import com.company.crm.AbstractUiTest;
import com.company.crm.view.main.MainView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainViewTest extends AbstractUiTest {

    @Test
    void opensMainView() {
        var view = viewTestSupport.navigateTo(MainView.class);
        assertThat(view).isInstanceOf(MainView.class);
    }
}
