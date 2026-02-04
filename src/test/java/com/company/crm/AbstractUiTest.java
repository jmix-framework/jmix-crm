package com.company.crm;

import com.company.crm.util.ViewTestSupport;
import io.jmix.flowui.testassist.FlowuiTestAssistConfiguration;
import io.jmix.flowui.testassist.UiTest;
import io.jmix.flowui.view.View;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@UiTest
@Import({FlowuiTestAssistConfiguration.class, ViewTestSupport.class})
public class AbstractUiTest extends AbstractTest {

    @Autowired
    protected ViewTestSupport viewTestSupport;

    protected <V extends View<?>> void assertCurrentView(Class<V> viewClass) {
        Assertions.assertEquals(viewClass, viewTestSupport.currentView().getClass());
    }
}
