package com.company.crm.view.component.aiconversation;

import io.jmix.flowui.xml.layout.loader.AbstractComponentLoader;
import org.dom4j.Element;

/**
 * Component loader for AiConversationComponent XML integration.
 */
public class AiConversationComponentLoader extends AbstractComponentLoader<AiConversationComponent> {

    @Override
    protected AiConversationComponent createComponent() {
        return factory.create(AiConversationComponent.class);
    }

    @Override
    public void loadComponent() {
        loadString(element, "conversationId")
            .ifPresent(resultComponent::setConversationId);

        loadConfiguration(resultComponent, element);

        // Load standard size attributes (width, height, min/max variants)
        componentLoader().loadSizeAttributes(resultComponent, element);

        // Load other common attributes
        componentLoader().loadClassNames(resultComponent, element);
        componentLoader().loadEnabled(resultComponent, element);
    }

    protected void loadConfiguration(AiConversationComponent component, Element element) {
        component.setHeaderVisible(loadBoolean(element, "showHeader").orElse(false));

        loadResourceString(element, "headerTitle", context.getMessageGroup(), component::setHeaderTitle);
        loadResourceString(element, "welcomeMessage", context.getMessageGroup(), component::setWelcomeMessage);
        loadResourceString(element, "assistantName", context.getMessageGroup(), component::setAssistantName);
        loadResourceString(element, "userName", context.getMessageGroup(), component::setUserName);
    }
}