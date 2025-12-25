package com.company.crm;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.util.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(AuthenticatedAsAdmin.class)
@SpringBootTest(classes = {CRMApplication.class})
@ActiveProfiles(CrmConstants.SpringProfiles.TEST)
public class AbstractTest {

    @Autowired
    protected DataManager dataManager;
}
