package com.company.crm;

import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.testassist.FlowuiTestAssistConfiguration;
import io.jmix.flowui.testassist.UiTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@UiTest
@Import(FlowuiTestAssistConfiguration.class)
public class AbstractUiTest extends AbstractTest {

    @Autowired
    protected ViewNavigators viewNavigators;
}
