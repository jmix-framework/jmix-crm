package com.company.crm.app.service.jmx;

import com.company.crm.app.annotation.DevProfile;
import com.company.crm.app.util.init.DemoDataInitializer;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@DevProfile
@Component
@ManagedResource(
        description = "Initialize demo data",
        objectName = "crm.jmx:type=DemoDataInitializer"
)
public class DemoDataInitializerMBean {

    private final DemoDataInitializer demoDataInitializer;

    public DemoDataInitializerMBean(DemoDataInitializer demoDataInitializer) {
        this.demoDataInitializer = demoDataInitializer;
    }

    @ManagedOperation(description = "Re-initialize demo data")
    public void resetDemoData() {
        demoDataInitializer.resetDemoData();
    }
}
