package com.company.crm.security;

import io.jmix.security.role.annotation.ResourceRole;

@ResourceRole(name = "HeadOfSales", code = HeadOfSalesRole.CODE)
public interface HeadOfSalesRole extends UiMinimalRole {
    String CODE = "head-of-sales";
}