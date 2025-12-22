package com.company.crm;

import com.company.crm.test_support.AuthenticatedAsAdmin;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class AbstractTest {
}
