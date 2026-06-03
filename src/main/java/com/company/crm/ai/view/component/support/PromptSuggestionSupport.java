package com.company.crm.ai.view.component.support;

import com.company.crm.ai.view.component.card.AiPromptSuggestionCard;
import com.company.crm.ai.context.AiContextEntityDefinition;
import com.company.crm.ai.model.AiPromptSuggestion;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.view.MessageBundle;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class PromptSuggestionSupport {

    private static final int INITIAL_SUGGESTION_COUNT = 10;
    private static final int ENTITY_SUGGESTION_COUNT = 4;

    private final UiComponents uiComponents;
    private final DataManager dataManager;
    private final IdSerialization idSerialization;
    private final MessageBundle messageBundle;
    private final Consumer<String> submitPrompt;

    public PromptSuggestionSupport(UiComponents uiComponents,
                            DataManager dataManager,
                            IdSerialization idSerialization,
                            MessageBundle messageBundle,
                            Consumer<String> submitPrompt) {
        this.uiComponents = uiComponents;
        this.dataManager = dataManager;
        this.idSerialization = idSerialization;
        this.messageBundle = messageBundle;
        this.submitPrompt = submitPrompt;
    }

    List<AiPromptSuggestion> selectInitialSuggestions() {
        List<AiPromptSuggestion> shuffled = new ArrayList<>(createPromptSuggestionPool());
        Collections.shuffle(shuffled);
        return shuffled.stream()
                .limit(4)
                .toList();
    }

    public List<AiPromptSuggestion> selectSuggestionsForEntityReferences(List<String> entityReferences) {
        if (entityReferences == null || entityReferences.isEmpty()) {
            return selectInitialSuggestions();
        }

        List<? extends Class<?>> distinctClasses = entityReferences.stream()
            .map(this::resolveEntityClass)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        return switch (distinctClasses.size()) {
            case 0 -> selectInitialSuggestions();
            case 1 -> distinctClasses.stream()
                .findFirst()
                .map(entityClass -> createEntityTypeSuggestions(entityClass, entityReferences.size()))
                .orElseGet(this::selectInitialSuggestions);
            default -> createMixedEntitySuggestions(entityReferences.size());
        };
    }

    public Card createPromptSuggestionCard(AiPromptSuggestion suggestion) {
        AiPromptSuggestionCard card = uiComponents.create(AiPromptSuggestionCard.class);
        card.setSuggestion(suggestion, submitPrompt);
        return card;
    }

    private List<AiPromptSuggestion> createPromptSuggestionPool() {
        return IntStream.rangeClosed(1, INITIAL_SUGGESTION_COUNT)
                .mapToObj(index -> createPromptSuggestion(
                        messageBundle.getMessage("promptSuggestion." + index + ".title"),
                        messageBundle.getMessage("promptSuggestion." + index + ".prompt")
                ))
                .toList();
    }

    private Class<?> resolveEntityClass(String entityReference) {
        try {
            Id<Object> id = idSerialization.stringToId(entityReference);
            return id.getEntityClass();
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<AiPromptSuggestion> createEntityTypeSuggestions(Class<?> entityClass, int selectionCount) {
        return AiContextEntityDefinition.findByEntityClass(entityClass)
                .map(definition -> createEntitySuggestions(definition.suggestionKey(), selectionCount))
                .orElseGet(() -> createEntitySuggestions("generic", selectionCount));
    }

    private List<AiPromptSuggestion> createMixedEntitySuggestions(int selectionCount) {
        return createEntitySuggestions("mixed", selectionCount);
    }

    private List<AiPromptSuggestion> createEntitySuggestions(String entityKey, int selectionCount) {
        String prefix = "promptSuggestion.entity." + entityKey + ".";
        String target = messageBundle.getMessage(prefix + (selectionCount == 1 ? "target.singular" : "target.plural"));
        return IntStream.rangeClosed(1, ENTITY_SUGGESTION_COUNT)
                .mapToObj(index -> createPromptSuggestion(
                        messageBundle.getMessage(prefix + index + ".title"),
                        messageBundle.formatMessage(prefix + index + ".prompt", target)
                ))
                .toList();
    }

    private AiPromptSuggestion createPromptSuggestion(String title, String prompt) {
        AiPromptSuggestion suggestion = dataManager.create(AiPromptSuggestion.class);
        suggestion.setTitle(title);
        suggestion.setPrompt(prompt);
        return suggestion;
    }
}
