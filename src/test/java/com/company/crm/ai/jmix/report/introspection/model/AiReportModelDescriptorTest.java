package com.company.crm.ai.jmix.report.introspection.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportModelDescriptorTest {

    @Test
    void shouldConstructFullModelWithNestedStructures() {
        // Arrange
        AiReportParameterDescriptor parameter = new AiReportParameterDescriptor(
                "client", "Client Parameter", "ENTITY", true, false, 
                "crm_Client", null, null);

        AiReportTemplateDescriptor template = new AiReportTemplateDescriptor(
                "DEFAULT", "XLSX", true);

        AiReportDescriptor report = new AiReportDescriptor(
                "rev_report", "Monthly Revenue", "Detailed financial report", "Finance", 
                List.of(template), List.of(parameter));

        AiReportModelDescriptor model = new AiReportModelDescriptor(Map.of("rev_report", report));

        // Act & Assert
        assertThat(model.reports()).containsKey("rev_report");
        AiReportDescriptor actualReport = model.reports().get("rev_report");
        
        assertThat(actualReport.code()).isEqualTo("rev_report");
        assertThat(actualReport.name()).isEqualTo("Monthly Revenue");
        assertThat(actualReport.description()).isEqualTo("Detailed financial report");
        assertThat(actualReport.group()).isEqualTo("Finance");
        
        assertThat(actualReport.templates()).hasSize(1);
        assertThat(actualReport.templates().get(0).code()).isEqualTo("DEFAULT");
        assertThat(actualReport.templates().get(0).isDefault()).isTrue();
        
        assertThat(actualReport.parameters()).hasSize(1);
        assertThat(actualReport.parameters().get(0).alias()).isEqualTo("client");
        assertThat(actualReport.parameters().get(0).required()).isTrue();
    }

    @Test
    void recordsShouldBeImmutable() {
        // Records are inherently immutable in Java
        AiReportTemplateDescriptor template = new AiReportTemplateDescriptor("T1", "PDF", false);
        // There are no setters, only accessors
        assertThat(template.code()).isEqualTo("T1");
    }
}
