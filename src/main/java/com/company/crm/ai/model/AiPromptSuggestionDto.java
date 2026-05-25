package com.company.crm.ai.model;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;

import java.util.UUID;

// TODO: rename -Dto - einfach AiPromptSuggestion
@JmixEntity(name = "crm_AiPromptSuggestionDto")
public class AiPromptSuggestionDto {

    @JmixId
    @JmixGeneratedValue
    private UUID id;

    @InstanceName
    private String title;

    private String prompt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
