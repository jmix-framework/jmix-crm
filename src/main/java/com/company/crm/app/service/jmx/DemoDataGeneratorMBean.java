package com.company.crm.app.service.jmx;

import com.company.crm.app.annotation.DevProfile;
import com.company.crm.app.util.init.DemoDataGenerator;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@DevProfile
@Component
@ManagedResource(
        description = "Generate demo data",
        objectName = "crm.jmx:type=DemoDataGenerator"
)
public class DemoDataGeneratorMBean {

    private final DemoDataGenerator demoDataGenerator;

    public DemoDataGeneratorMBean(DemoDataGenerator demoDataGenerator) {
        this.demoDataGenerator = demoDataGenerator;
    }

    @ManagedOperation(description = "Re-generate demo data")
    public void regenerateData() {
        demoDataGenerator.resetDemoData();
    }
}
