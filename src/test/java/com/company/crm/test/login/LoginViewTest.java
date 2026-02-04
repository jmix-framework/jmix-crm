package com.company.crm.test.login;

import com.company.crm.AbstractUiTest;
import com.company.crm.view.login.LoginView;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginViewTest extends AbstractUiTest {

    @Test
    void opensLoginView() {
        var view = viewTestSupport.navigateTo(LoginView.class);
        assertThat(view).isInstanceOf(LoginView.class);
    }

    @Test
    void successLogin() {
        viewTestSupport.navigateToAnd(LoginView.class, loginView -> {
            fillFormAndLogin("admin", "admin");
            assertCurrentView(MainView.class);
        });
    }

    @Test
    void failedLogin() {
        viewTestSupport.navigateToAnd(LoginView.class, loginView -> {
            fillFormAndLogin("username", "password");
            assertCurrentView(LoginView.class);
        });
    }

    private void fillFormAndLogin(String username, String password) {
        viewTestSupport.setComponentValue("usernameField", username);
        viewTestSupport.setComponentValue("passwordField", password);
        viewTestSupport.<Button>getComponent("submitBtn").click();
    }
}
