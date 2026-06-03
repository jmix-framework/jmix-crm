package com.company.crm.ai.view.component.card;

import com.company.crm.ai.view.component.support.AiContextCardActionSurface;
import com.company.crm.ai.model.AiPromptSuggestion;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.function.Consumer;

public class AiPromptSuggestionCard extends Card {

    public AiPromptSuggestionCard() {
        setWidthFull();
        addClassName("ai-conversation-starter-suggestion-card");
        addThemeVariants(CardVariant.LUMO_OUTLINED);
    }

    public void setSuggestion(AiPromptSuggestion suggestion, Consumer<String> submitPrompt) {
        getElement().setProperty("title", """
                %s
                %s""".formatted(suggestion.getTitle(), suggestion.getPrompt()));

        AiContextCardActionSurface actionSurface = new AiContextCardActionSurface();
        actionSurface.configure(
                VaadinIcon.LIGHTBULB.create(),
                suggestion.getTitle(),
                suggestion.getPrompt(),
                "ai-conversation-starter-suggestion-card-icon",
                "font-semibold text-m ai-conversation-starter-suggestion-card-title",
                "text-m text-secondary ai-conversation-starter-suggestion-card-prompt",
                () -> submitPrompt.accept(suggestion.getPrompt())
        );

        add(actionSurface);
    }
}
