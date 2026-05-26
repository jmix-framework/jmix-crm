package com.company.crm.test.ai.prompt;

import com.company.crm.ai.prompt.AiPromptContentBuilder;
import org.junit.jupiter.api.Test;

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
}
