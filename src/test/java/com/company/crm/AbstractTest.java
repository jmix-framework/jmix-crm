package com.company.crm;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.util.Entities;
import com.company.crm.util.extenstion.AuthenticatedAsAdmin;
import com.company.crm.util.extenstion.DataCleaner;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.function.Consumer;

@ExtendWith({AuthenticatedAsAdmin.class, DataCleaner.class})
@ActiveProfiles(CrmConstants.SpringProfiles.TEST)
@SpringBootTest(
        classes = {CRMApplication.class, Entities.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AbstractTest {

    @LocalServerPort
    protected int port = 0;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected DataManager dataManager;

    @Autowired
    protected Entities entities;

    @BeforeAll
    public static void beforeAll() {

    }

    @AfterAll
    public static void afterAll() {

    }

    @BeforeEach
    public final void doBeforeEach() {
        beforeEach();
    }

    @AfterEach
    public final void doAfterEach() {
        afterEach();
    }

    protected void beforeEach() {
    }

    protected void afterEach() {
    }

    protected boolean cleanDataAfterEach() {
        return true;
    }

    protected <E> E createAndSaveEntity(Class<E> entityClass, Consumer<E> creation) {
        return entities.createAndSaveEntity(entityClass, creation);
    }

    protected <E> E createEntity(Class<E> entityClass) {
        return entities.createEntity(entityClass);
    }

    protected <E> E saveWithoutReload(E entity) {
        return entities.saveWithoutReload(entity);
    }
}
