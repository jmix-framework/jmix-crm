package com.company.crm.test.login;

import com.company.crm.AbstractUiTest;
import com.company.crm.view.login.LoginView;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.login.AbstractLogin;
import io.jmix.flowui.component.loginform.JmixLoginForm;
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
            viewTestSupport.<JmixLoginForm>getComponentAnd("login", loginForm ->
                    ComponentUtil.fireEvent(loginForm, new AbstractLogin.LoginEvent(loginForm, true, "admin", "admin")));
        });
        assertCurrentView(MainView.class);
    }

    @Test
    void failedLogin() {
        viewTestSupport.navigateToAnd(LoginView.class, loginView -> {
            viewTestSupport.<JmixLoginForm>getComponentAnd("login", loginForm ->
                    ComponentUtil.fireEvent(loginForm, new AbstractLogin.LoginEvent(loginForm, true, "unknow", "unknown")));
        });
        assertCurrentView(LoginView.class);
    }
}
