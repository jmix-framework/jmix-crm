package com.company.crm.test.usertask;

import com.company.crm.AbstractUiTest;
import com.company.crm.view.usertask.UserTaskListView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskViewsTest extends AbstractUiTest {

    @Test
    void opensUserTaskListView() {
        var view = viewTestSupport.navigateTo(UserTaskListView.class);
        assertThat(view).isInstanceOf(UserTaskListView.class);
    }
}
