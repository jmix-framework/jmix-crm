package com.company.crm.test.user;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.user.User;
import com.company.crm.view.user.UserDetailView;
import com.company.crm.view.user.UserListView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserViewsTest extends AbstractUiTest {

    @Test
    void opensUserListView() {
        var view = viewTestSupport.navigateTo(UserListView.class);
        assertThat(view).isInstanceOf(UserListView.class);
    }

    @Test
    void opensUserDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(User.class, UserDetailView.class);
        assertThat(view).isInstanceOf(UserDetailView.class);
    }
}
