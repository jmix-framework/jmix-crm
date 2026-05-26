package com.company.crm.ai.prompt;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public AiPromptContentBuilder appendParagraphIf(boolean condition, String text) {
        if (condition) {
            return appendParagraph(text);
        }
        return this;
    }

    public AiPromptContentBuilder appendParagraphIf(boolean condition, Supplier<String> textSupplier) {
        if (condition && textSupplier != null) {
            return appendParagraph(textSupplier.get());
        }
        return this;
    }

    public AiPromptContentBuilder appendParagraphs(Collection<String> paragraphs) {
        if (paragraphs != null) {
            paragraphs.forEach(this::appendParagraph);
        }
        return this;
    }

    public AiPromptContentBuilder appendParagraphs(Stream<String> paragraphs) {
        if (paragraphs != null) {
            paragraphs.forEach(this::appendParagraph);
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

    public AiPromptContentBuilder appendSectionIf(boolean condition, String title, String body) {
        if (condition) {
            return appendSection(title, body);
        }
        return this;
    }

    public AiPromptContentBuilder appendSectionIf(boolean condition, String title, Supplier<String> bodySupplier) {
        if (condition && bodySupplier != null) {
            return appendSection(title, bodySupplier.get());
        }
        return this;
    }

    public AiPromptContentBuilder appendSection(String title, List<String> bodyBlocks) {
        if (bodyBlocks == null || bodyBlocks.isEmpty()) {
            return this;
        }
        String body = bodyBlocks.stream()
                .filter(StringUtils::hasText)
                .map(String::strip)
                .collect(Collectors.joining("\n\n"));
        return appendSection(title, body);
    }

    public AiPromptContentBuilder appendSection(String title, Stream<String> bodyBlocks) {
        if (bodyBlocks == null) {
            return this;
        }
        return appendSection(title, bodyBlocks.toList());
    }

    public AiPromptContentBuilder appendFields(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return this;
        }
        String fieldsString = fields.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue()))
                .map(entry -> "%s: %s".formatted(entry.getKey().strip(), entry.getValue().strip()))
                .collect(Collectors.joining("\n"));
        return appendParagraph(fieldsString);
    }


    public String build() {
        return String.join("\n\n", blocks);
    }
}
