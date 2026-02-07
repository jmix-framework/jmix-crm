package com.company.crm.util.extenstion;

import com.company.crm.security.role.ManagerRole;
import com.company.crm.security.role.SupervisorRole;
import com.company.crm.util.TestUsers;
import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Optional;

public class AuthenticatedAsExtension implements BeforeEachCallback, AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AuthenticatedAsExtension.class);
    private static final String AUTHENTICATED = "authenticated";
    private static final String AUTHENTICATED_BEFORE_EACH = "authenticated_before_each";

    @Override
    public void beforeEach(ExtensionContext context) {
        var authenticatedAs = getAnnotation(context).orElse(AuthenticatedAs.ADMIN);
        String username = authenticatedAs.value();
        ensureUser(context, username);
        getSystemAuthenticator(context).begin(username);
        context.getStore(NAMESPACE).put(AUTHENTICATED_BEFORE_EACH, true);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (context.getStore(NAMESPACE).remove(AUTHENTICATED_BEFORE_EACH, Boolean.class) == Boolean.TRUE) {
            getSystemAuthenticator(context).end();
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        var authenticatedAs = getAnnotation(context).orElse(AuthenticatedAs.ADMIN);
        String username = authenticatedAs.value();
        ensureUser(context, username);
        getSystemAuthenticator(context).begin(username);
        context.getStore(NAMESPACE).put(AUTHENTICATED, true);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getStore(NAMESPACE).remove(AUTHENTICATED, Boolean.class) == Boolean.TRUE) {
            getSystemAuthenticator(context).end();
        }
    }

    private void ensureUser(ExtensionContext context, String username) {
        TestUsers testUsers = ExtensionUtils.getBean(context, TestUsers.class);
        if (ManagerRole.CODE.equals(username)) {
            testUsers.manager();
        } else if (SupervisorRole.CODE.equals(username)) {
            testUsers.supervisor();
        } else {
            testUsers.ensureUser(username);
        }
    }

    private Optional<AuthenticatedAs> getAnnotation(ExtensionContext context) {
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(context.getRequiredTestMethod(), AuthenticatedAs.class))
                .or(() -> Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(context.getRequiredTestClass(), AuthenticatedAs.class)));
    }

    private SystemAuthenticator getSystemAuthenticator(ExtensionContext context) {
        return ExtensionUtils.getBean(context, SystemAuthenticator.class);
    }
}
