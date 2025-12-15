package com.company.crm.security;

import io.jmix.security.role.annotation.ResourceRole;

@ResourceRole(name = "Manager", code = ManagerRole.CODE, scope = "UI")
public interface ManagerRole {
    String CODE = "manager";
}