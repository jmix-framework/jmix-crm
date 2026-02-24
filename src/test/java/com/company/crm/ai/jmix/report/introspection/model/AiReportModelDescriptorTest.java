package com.company.crm.ai.jmix.report.introspection.model;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiReportModelDescriptorTest {

    @Test
    void shouldConstructFullModelWithNestedStructures() {
        // given
        AiReportParameterDescriptor parameter = new AiReportParameterDescriptor(
                "client", "Client Parameter", "ENTITY", true, false, 
                "crm_Client", null, null);

        AiReportTemplateDescriptor template = new AiReportTemplateDescriptor(
                "DEFAULT", "XLSX", true);

        AiReportDescriptor report = new AiReportDescriptor(
                "rev_report", "Monthly Revenue", "Detailed financial report", "Finance", 
                List.of(template), List.of(parameter));

        AiReportModelDescriptor model = new AiReportModelDescriptor(Map.of("rev_report", report));

        // when
        Map<String, AiReportDescriptor> reports = model.reports();

        // then
        assertThat(reports).containsKey("rev_report");
        AiReportDescriptor actualReport = reports.get("rev_report");
        
        assertThat(actualReport.code()).isEqualTo("rev_report");
        assertThat(actualReport.name()).isEqualTo("Monthly Revenue");
        assertThat(actualReport.description()).isEqualTo("Detailed financial report");
        assertThat(actualReport.group()).isEqualTo("Finance");
        assertThat(actualReport.templates())
                .extracting(AiReportTemplateDescriptor::code, AiReportTemplateDescriptor::outputType)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("DEFAULT", "XLSX"));
        assertThat(actualReport.parameters())
                .extracting(AiReportParameterDescriptor::alias, AiReportParameterDescriptor::type)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("client", "ENTITY"));
        assertThat(actualReport.templates().getFirst().isDefault()).isTrue();
        assertThat(actualReport.parameters().getFirst().required()).isTrue();
    }

    @Test
    void recordsShouldBeImmutable() {
        // given
        AiReportTemplateDescriptor template = new AiReportTemplateDescriptor("T1", "PDF", false);
        AiReportDescriptor report = new AiReportDescriptor(
                "rep1", "Rep 1", "Desc", "Group", List.of(template), List.of());
        AiReportModelDescriptor model = new AiReportModelDescriptor(Map.of("rep1", report));

        // when
        AiReportDescriptor firstRead = model.reports().get("rep1");
        AiReportDescriptor secondRead = model.reports().get("rep1");

        // then
        assertThat(template.code()).isEqualTo("T1");
        assertThat(firstRead).isSameAs(secondRead);
        assertThat(firstRead.templates().getFirst().code()).isEqualTo("T1");
        assertThatThrownBy(() -> firstRead.templates().add(new AiReportTemplateDescriptor("T2", "XLSX", true)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> model.reports().put("rep2", report))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
