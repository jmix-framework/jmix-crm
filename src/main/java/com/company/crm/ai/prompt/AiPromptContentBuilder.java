package com.company.crm.ai.prompt;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AiPromptContentBuilder {

    private final List<String> blocks = new ArrayList<>();

    public static AiPromptContentBuilder create() {
        return new AiPromptContentBuilder();
    }

    public AiPromptContentBuilder appendParagraph(String text) {
        if (StringUtils.hasText(text)) {
            blocks.add(text.strip());
        }
        return this;
    }

    public AiPromptContentBuilder appendSection(String title, String body) {
        if (!StringUtils.hasText(body)) {
            return this;
        }
        String sectionTitle = StringUtils.hasText(title) ? title.strip() : "Context";
        return appendParagraph("%s:%n%n%s".formatted(sectionTitle, body.strip()));
    }

    public String build() {
        return String.join("\n\n", blocks);
    }
}
