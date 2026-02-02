package com.company.crm;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractServiceTest<S> extends AbstractTest {
    @Autowired
    protected S service;
}
