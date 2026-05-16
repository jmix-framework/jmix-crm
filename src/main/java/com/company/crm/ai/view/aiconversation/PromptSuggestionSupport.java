package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiPromptSuggestionDto;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.DataManager;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.view.MessageBundle;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

class PromptSuggestionSupport {

    private final UiComponents uiComponents;
    private final DataManager dataManager;
    private final MessageBundle messageBundle;
    private final Consumer<String> submitPrompt;

    PromptSuggestionSupport(UiComponents uiComponents,
                            DataManager dataManager,
                            MessageBundle messageBundle,
                            Consumer<String> submitPrompt) {
        this.uiComponents = uiComponents;
        this.dataManager = dataManager;
        this.messageBundle = messageBundle;
        this.submitPrompt = submitPrompt;
    }

    List<AiPromptSuggestionDto> selectInitialSuggestions() {
        List<AiPromptSuggestionDto> shuffled = new ArrayList<>(createPromptSuggestionPool());
        Collections.shuffle(shuffled);
        return shuffled.stream()
                .limit(4)
                .toList();
    }

    Card createPromptSuggestionCard(AiPromptSuggestionDto suggestion) {
        Card card = uiComponents.create(Card.class);
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_OUTLINED);
        card.getElement().addEventListener("click", event -> submitPrompt.accept(suggestion.getPrompt()));
        card.getElement().setProperty("title", suggestion.getTitle() + "\n" + suggestion.getPrompt());
        card.getStyle().set("cursor", "pointer");
        card.getStyle().set("overflow", "hidden");

        Icon icon = VaadinIcon.LIGHTBULB.create();
        icon.getStyle().set("flex-shrink", "0");
        icon.getStyle().set("color", "var(--lumo-primary-color)");
        card.setHeaderPrefix(icon);

        Span titleText = uiComponents.create(Span.class);
        titleText.setText(suggestion.getTitle());
        titleText.addClassNames("font-semibold", "text-m");
        titleText.getStyle().set("display", "block");
        titleText.getStyle().set("overflow", "hidden");
        titleText.getStyle().set("text-overflow", "ellipsis");
        titleText.getStyle().set("white-space", "nowrap");
        titleText.getElement().setProperty("title", suggestion.getTitle());
        card.setTitle(titleText);

        Span promptText = uiComponents.create(Span.class);
        promptText.setText(suggestion.getPrompt());
        promptText.addClassNames("text-m", "text-secondary");
        promptText.getStyle().set("display", "-webkit-box");
        promptText.getStyle().set("-webkit-line-clamp", "2");
        promptText.getStyle().set("-webkit-box-orient", "vertical");
        promptText.getStyle().set("overflow", "hidden");
        promptText.getStyle().set("text-overflow", "ellipsis");
        promptText.getStyle().set("line-height", "1.4");
        promptText.getStyle().set("max-height", "2.8em");
        promptText.getElement().setProperty("title", suggestion.getPrompt());

        VerticalLayout textLayout = uiComponents.create(VerticalLayout.class);
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.setWidthFull();
        textLayout.addClassNames(LumoUtility.Gap.XSMALL);
        textLayout.add(promptText);
        textLayout.setMargin(false);

        card.add(textLayout);
        return card;
    }

    private List<AiPromptSuggestionDto> createPromptSuggestionPool() {
        List<AiPromptSuggestionDto> suggestions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String title = messageBundle.getMessage("promptSuggestion." + i + ".title");
            String prompt = messageBundle.getMessage("promptSuggestion." + i + ".prompt");
            suggestions.add(createPromptSuggestion(title, prompt));
        }
        return suggestions;
    }

    private AiPromptSuggestionDto createPromptSuggestion(String title, String prompt) {
        AiPromptSuggestionDto suggestion = dataManager.create(AiPromptSuggestionDto.class);
        suggestion.setTitle(title);
        suggestion.setPrompt(prompt);
        return suggestion;
    }
}
