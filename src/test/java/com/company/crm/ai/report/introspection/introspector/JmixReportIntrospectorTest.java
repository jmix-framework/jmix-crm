package com.company.crm.ai.report.introspection.introspector;

import com.company.crm.AbstractTest;
import com.company.crm.ai.report.introspection.introspector.JmixReportIntrospector;
import com.company.crm.ai.report.introspection.model.AiReportDescriptor;
import com.company.crm.ai.report.introspection.model.AiReportModelDescriptor;
import com.company.crm.ai.report.introspection.model.AiReportParameterDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JmixReportIntrospectorTest extends AbstractTest {

    @Autowired
    private JmixReportIntrospector introspector;

    @Test
    void shouldIntrospectClient360Report() {
        // given

        // when
        AiReportModelDescriptor model = introspector.introspect();

        // then
        assertThat(model.reports()).containsKey("client-360-report");
        AiReportDescriptor descriptor = model.reports().get("client-360-report");

        assertThat(descriptor.code()).isEqualTo("client-360-report");
        assertThat(descriptor.name()).isEqualTo("Client 360 Report");
        assertThat(descriptor.description())
                .contains("Comprehensive 360-degree view of a client")
                .contains("financial risk assessment")
                .contains("business indicators")
                .contains("holistic overview");
        
        // Templates
        assertThat(descriptor.templates()).isNotEmpty();
        assertThat(descriptor.templates().stream().anyMatch(t -> t.isDefault())).isTrue();

        // Parameters
        assertThat(descriptor.parameters())
                .hasSize(4)
                .extracting(AiReportParameterDescriptor::alias)
                .containsExactlyInAnyOrder("client", "fromDate", "toDate", "audience");
        assertThat(descriptor.parameters())
                .extracting(AiReportParameterDescriptor::alias)
                .doesNotHaveDuplicates();
        
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

        Optional<AiReportParameterDescriptor> audienceParam = descriptor.parameters().stream()
                .filter(p -> p.alias().equals("audience"))
                .findFirst();
        assertThat(audienceParam).isPresent();
        assertThat(audienceParam.get().type()).isNotBlank();
    }

    @Test
    void shouldIntrospectOnlyRequestedReports() {
        // given
        List<String> requestedCodes = List.of("client-360-report");

        // when
        AiReportModelDescriptor model = introspector.introspect(requestedCodes);

        // then
        assertThat(model.reports()).hasSize(1);
        assertThat(model.reports()).containsKey("client-360-report");
        assertThat(model.reports()).doesNotContainKey("invoice-report");
    }
}
