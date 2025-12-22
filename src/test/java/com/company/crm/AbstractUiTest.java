package com.company.crm;

import io.jmix.flowui.testassist.FlowuiTestAssistConfiguration;
import io.jmix.flowui.testassist.UiTest;
import org.springframework.boot.test.context.SpringBootTest;

@UiTest
@SpringBootTest(classes = {CRMApplication.class, FlowuiTestAssistConfiguration.class})
public class AbstractUiTest extends AbstractTest {
}
