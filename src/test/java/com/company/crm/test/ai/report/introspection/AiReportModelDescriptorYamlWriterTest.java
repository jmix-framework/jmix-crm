package com.company.crm.test.ai.report.introspection;

import com.company.crm.ai.report.introspection.AiReportModelDescriptorYamlWriter;
import com.company.crm.ai.report.introspection.model.AiReportDescriptor;
import com.company.crm.ai.report.introspection.model.AiReportModelDescriptor;
import com.company.crm.ai.report.introspection.model.AiReportParameterDescriptor;
import com.company.crm.ai.report.introspection.model.AiReportTemplateDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportModelDescriptorYamlWriterTest {

    private final AiReportModelDescriptorYamlWriter writer = new AiReportModelDescriptorYamlWriter();

    @Test
    void shouldWriteEmptyModel() {
        // given
        AiReportModelDescriptor model = new AiReportModelDescriptor(Map.of());

        // when
        String yaml = writer.writeToYaml(model);

        // then
        assertThat(yaml).contains("{}");
    }

    @Test
    void shouldWriteComplexModelWithCorrectFormatting() {
        // given
        AiReportParameterDescriptor parameter = new AiReportParameterDescriptor(
                "client", "Client Parameter", "ENTITY", true, false,
                "crm_Client", null, null);

        AiReportTemplateDescriptor template = new AiReportTemplateDescriptor(
                "DEFAULT", "XLSX", true);

        // Use TreeMap to ensure deterministic YAML output for testing
        Map<String, AiReportDescriptor> reports = new TreeMap<>();
        reports.put("rev_report", new AiReportDescriptor(
                "rev_report", "Monthly Revenue", "Revenue analytics", "Finance",
                List.of(template), List.of(parameter)));

        AiReportModelDescriptor model = new AiReportModelDescriptor(reports);

        // when
        String yaml = writer.writeToYaml(model);

        // then
        assertThat(yaml).contains("reports:");
        assertThat(yaml).contains("rev_report:");
        assertThat(yaml).contains("code: rev_report");
        assertThat(yaml).contains("name: Monthly Revenue");
        assertThat(yaml).contains("description: Revenue analytics");
        assertThat(yaml).contains("group: Finance");
        assertThat(yaml).contains("templates:");
        assertThat(yaml).contains("- code: DEFAULT");
        assertThat(yaml).contains("outputType: XLSX");
        assertThat(yaml).contains("isDefault: true");
        assertThat(yaml).contains("parameters:");
        assertThat(yaml).contains("- alias: client");
        assertThat(yaml).contains("name: Client Parameter");
        assertThat(yaml).contains("type: ENTITY");
        assertThat(yaml).contains("required: true");
        assertThat(yaml).contains("entityMetaClass: crm_Client");
        assertThat(yaml).contains("""
                    parameters:
                      - alias: client
                        name: Client Parameter
                        type: ENTITY
                        required: true
                        hidden: false
                        entityMetaClass: crm_Client
                """);
    }

    @Test
    void shouldExcludeEmptyFields() {
        // given
        AiReportParameterDescriptor parameter = new AiReportParameterDescriptor(
                "simple", "Simple Param", "String", false, false,
                null, null, null);

        AiReportDescriptor report = new AiReportDescriptor(
                "simple", "Simple Report", null, null, List.of(), List.of(parameter));

        AiReportModelDescriptor model = new AiReportModelDescriptor(Map.of("simple", report));

        // when
        String yaml = writer.writeToYaml(model);

        // then
        assertThat(yaml).contains("code: simple");
        assertThat(yaml).contains("name: Simple Report");
        assertThat(yaml).contains("parameters:");
        assertThat(yaml).contains("- alias: simple");
        assertThat(yaml).contains("type: String");
        assertThat(yaml).contains("required: false");
        assertThat(yaml).doesNotContain("group:");
        assertThat(yaml).doesNotContain("templates:");
        assertThat(yaml).doesNotContain("entityMetaClass:");
        assertThat(yaml).doesNotContain("enumerationClass:");
        assertThat(yaml).doesNotContain("defaultValue:");
    }
}
