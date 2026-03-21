package com.company.crm.security.role;

import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "AnonymousRole", code = AnonymousRole.CODE, scope = "UI")
public interface AnonymousRole {
    String CODE = "anonymous-role";

    @ViewPolicy(viewIds = "crm_UsageHelpView")
    void screens();
}
