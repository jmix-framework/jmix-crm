package com.company.crm.test.security;

import com.company.crm.AbstractTest;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.security.role.ManagerRole;
import com.company.crm.security.role.SupervisorRole;
import com.company.crm.security.role.UiMinimalRole;
import com.company.crm.util.TestUsers;
import io.jmix.core.AccessManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RoleViewAccessTest extends AbstractTest {

    @Autowired
    private AccessManager accessManager;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Autowired
    private TestUsers testUsers;

    @Test
    void uiMinimalRole_hasMinimalViewAccess() {
        var user = testUsers.ensureUser("ui-minimal-user");
        testUsers.assignRole(user.getUsername(), UiMinimalRole.CODE);

        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.MAIN, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.USER_DETAIL, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.LOGIN, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.HOME, false);
    }

    @Test
    void managerRole_allowsBusinessViews() {
        var user = testUsers.ensureUser("manager-user");
        testUsers.assignRole(user.getUsername(), ManagerRole.CODE);

        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.HOME, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.USER_TASK_LIST, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.CLIENT_LIST, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.ORDER_LIST, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.PAYMENT_LIST, true);

        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.CATEGORY_LIST, false);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.USER_LIST, false);
    }

    @Test
    void supervisorRole_allowsCatalogManagement() {
        var user = testUsers.ensureUser("supervisor-user");
        testUsers.assignRole(user.getUsername(), SupervisorRole.CODE);

        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.CATEGORY_LIST, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.CATEGORY_ITEM_LIST, true);
        assertViewAccess(user.getUsername(), CrmConstants.ViewIds.USER_LIST, false);
    }

    @Test
    void adminRole_allowsAllViews() {
        assertViewAccess("admin", CrmConstants.ViewIds.USER_LIST, true);
        assertViewAccess("admin", CrmConstants.ViewIds.CATEGORY_LIST, true);
        assertViewAccess("admin", CrmConstants.ViewIds.MAIN, true);
    }

    private void assertViewAccess(String username, String viewId, boolean expected) {
        systemAuthenticator.runWithUser(username, () -> {
            UiShowViewContext context = new UiShowViewContext(viewId);
            accessManager.applyRegisteredConstraints(context);
            assertThat(context.isPermitted()).isEqualTo(expected);
        });
    }
}
