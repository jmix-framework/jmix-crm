package com.company.crm.test.ai.prompt;

import com.company.crm.ai.prompt.AiPromptContentBuilder;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptContentBuilderTest {

    @Test
    void appendsParagraphsAndSectionsWithConsistentSpacing() {
        String prompt = AiPromptContentBuilder.create()
                .appendParagraph(" First paragraph ")
                .appendParagraph(" ")
                .appendSection("CRM Context", " Selected client ")
                .build();

        assertThat(prompt).isEqualTo("""
                First paragraph

                CRM Context:

                Selected client""");
    }

    @Test
    void appendsSectionFromMultipleBodyBlocks() {
        String prompt = AiPromptContentBuilder.create()
                .appendSection("Referenced CRM entities", List.of(
                        "client-1\n{\"name\":\"Acme\"}",
                        "client-2\n{\"name\":\"Globex\"}"
                ))
                .build();

        assertThat(prompt).isEqualTo("""
                Referenced CRM entities:

                client-1
                {"name":"Acme"}

                client-2
                {"name":"Globex"}""");
    }

    @Test
    void appendsSectionFromStream() {
        String prompt = AiPromptContentBuilder.create()
                .appendSection("Referenced CRM entities", Stream.of(
                        "client-1\n{\"name\":\"Acme\"}",
                        "client-2\n{\"name\":\"Globex\"}"
                ))
                .build();

        assertThat(prompt).isEqualTo("""
                Referenced CRM entities:

                client-1
                {"name":"Acme"}

                client-2
                {"name":"Globex"}""");
    }

    @Test
    void appendsFieldsFromMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Attached file", "client.csv");
        fields.put("MIME type", "text/csv");
        fields.put("Skipped key", "");
        fields.put("", "Skipped value");

        String prompt = AiPromptContentBuilder.create()
                .appendFields(fields)
                .build();

        assertThat(prompt).isEqualTo("""
                Attached file: client.csv
                MIME type: text/csv""");
    }

    @Test
    void appendsParagraphsFromCollectionAndStream() {
        String prompt = AiPromptContentBuilder.create()
                .appendParagraphs(List.of("Para 1", "Para 2"))
                .appendParagraphs(Stream.of("Para 3", "Para 4"))
                .build();

        assertThat(prompt).isEqualTo("""
                Para 1

                Para 2

                Para 3

                Para 4""");
    }

    @Test
    void appendsParagraphIfCondition() {
        String prompt = AiPromptContentBuilder.create()
                .appendParagraphIf(true, "Appended")
                .appendParagraphIf(false, "Ignored")
                .appendParagraphIf(true, () -> "Appended from Supplier")
                .appendParagraphIf(false, () -> "Ignored from Supplier")
                .build();

        assertThat(prompt).isEqualTo("""
                Appended

                Appended from Supplier""");
    }

    @Test
    void appendsSectionIfCondition() {
        String prompt = AiPromptContentBuilder.create()
                .appendSectionIf(true, "Sec 1", "Appended content")
                .appendSectionIf(false, "Sec 2", "Ignored content")
                .appendSectionIf(true, "Sec 3", () -> "Supplier content")
                .appendSectionIf(false, "Sec 4", () -> "Ignored supplier content")
                .build();

        assertThat(prompt).isEqualTo("""
                Sec 1:

                Appended content

                Sec 3:

                Supplier content""");
    }
}
