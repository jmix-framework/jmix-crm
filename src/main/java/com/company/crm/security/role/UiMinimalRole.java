package com.company.crm.security.role;

import com.company.crm.app.util.constant.CrmConstants;
import io.jmix.security.model.SecurityScope;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.security.role.annotation.SpecificPolicy;
import io.jmix.securityflowui.role.UiMinimalPolicies;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "UI: minimal access", code = UiMinimalRole.CODE, scope = SecurityScope.UI)
public interface UiMinimalRole extends UiMinimalPolicies {

    String CODE = "ui-minimal";

    @ViewPolicy(viewIds = {CrmConstants.ViewIds.MAIN, CrmConstants.ViewIds.USER_DETAIL, CrmConstants.ViewIds.ABOUT})
    void main();

    @ViewPolicy(viewIds = CrmConstants.ViewIds.LOGIN)
    @SpecificPolicy(resources = "ui.loginToUi")
    void login();
}
