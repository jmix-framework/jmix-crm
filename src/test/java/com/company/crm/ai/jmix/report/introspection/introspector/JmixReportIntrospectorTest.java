package com.company.crm.ai.jmix.report.introspection.introspector;

import com.company.crm.AbstractTest;
import com.company.crm.ai.jmix.report.introspection.model.AiReportDescriptor;
import com.company.crm.ai.jmix.report.introspection.model.AiReportModelDescriptor;
import com.company.crm.ai.jmix.report.introspection.model.AiReportParameterDescriptor;
import io.jmix.reports.ReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JmixReportIntrospectorTest extends AbstractTest {

    @Autowired
    private JmixReportIntrospector introspector;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void shouldIntrospectClient360Report() {
        // Act
        AiReportModelDescriptor model = introspector.introspect();

        // Assert
        assertThat(model.reports()).containsKey("client-360-report");
        AiReportDescriptor descriptor = model.reports().get("client-360-report");

        assertThat(descriptor.code()).isEqualTo("client-360-report");
        assertThat(descriptor.name()).isEqualTo("Client 360 Report");
        assertThat(descriptor.description()).contains("Comprehensive client report");
        
        // Templates
        assertThat(descriptor.templates()).isNotEmpty();
        assertThat(descriptor.templates().stream().anyMatch(t -> t.isDefault())).isTrue();

        // Parameters
        assertThat(descriptor.parameters()).hasSizeGreaterThanOrEqualTo(3);
        
        // Check Client Parameter (ENTITY)
        Optional<AiReportParameterDescriptor> clientParam = descriptor.parameters().stream()
                .filter(p -> p.alias().equals("client"))
                .findFirst();
        assertThat(clientParam).isPresent();
        assertThat(clientParam.get().type()).isEqualTo("ENTITY");
        assertThat(clientParam.get().entityMetaClass()).isEqualTo("Client");
        assertThat(clientParam.get().required()).isTrue();

        // Check FromDate Parameter (DATE)
        Optional<AiReportParameterDescriptor> fromDateParam = descriptor.parameters().stream()
                .filter(p -> p.alias().equals("fromDate"))
                .findFirst();
        assertThat(fromDateParam).isPresent();
        assertThat(fromDateParam.get().type()).isEqualTo("DATE");
    }

    @Test
    void shouldIntrospectOnlyRequestedReports() {
        // Act
        AiReportModelDescriptor model = introspector.introspect(List.of("client-360-report"));

        // Assert
        assertThat(model.reports()).hasSize(1);
        assertThat(model.reports()).containsKey("client-360-report");
        assertThat(model.reports()).doesNotContainKey("invoice-report");
    }
}
