package com.company.crm.view.login;

import com.company.crm.view.home.HomeView;
import com.vaadin.flow.router.BeforeEnterEvent;
import io.jmix.flowui.sys.LoginViewBeforeEnterHandler;
import io.jmix.flowui.sys.LoginViewRedirectSupport;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@SuppressWarnings("ALL")
public class CrmLoginViewBeforeEnterHandler extends LoginViewBeforeEnterHandler {

    public CrmLoginViewBeforeEnterHandler(LoginViewRedirectSupport loginViewRedirectSupport) {
        super(loginViewRedirectSupport);
    }

    @Override
    protected void handleAuthenticatedUser(BeforeEnterEvent event) {
        event.forwardTo(HomeView.class);
    }
}
