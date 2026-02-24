package com.company.crm.app.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CrmAnalyticsSystemPromptContractTest {

    @Test
    void promptContainsMarkdownHeadingConstraint() throws IOException {
        // given

        // when
        String prompt = loadPrompt();

        // then
        assertHeadingContract(prompt);
    }

    @Test
    void promptContainsBalancedFormattingGuidelines() throws IOException {
        // given

        // when
        String prompt = loadPrompt();

        // then
        assertFormattingContract(prompt);
    }

    private String loadPrompt() throws IOException {
        ClassPathResource resource = new ClassPathResource("prompts/crm-analytics-system-prompt.st");
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertHeadingContract(String prompt) {
        assertThat(prompt).contains("Use only ## (H2) and ### (H3) headings in markdown - never use # (H1) headings");
        assertThat(prompt).doesNotMatch("(?m)^#\\s");
    }

    private void assertFormattingContract(String prompt) {
        assertThat(prompt).contains("Prefer short prose paragraphs over bullet lists");
        assertThat(prompt).contains("Use headings sparingly");
        assertThat(prompt).contains("Do not produce heading/list skeletons");
        assertThat(prompt).contains("prefer a compact markdown table");
        assertThat(prompt).contains("Recommended response shape");
    }
}
