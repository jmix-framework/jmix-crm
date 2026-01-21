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
    }

    protected void loadConfiguration(AiConversationComponent component, Element element) {
        // Apply XML configuration directly to the component
        boolean showHeader = loadBoolean(element, "showHeader").orElse(false);
        String headerTitle = loadString(element, "headerTitle").orElse(null);
        String welcomeMessage = loadString(element, "welcomeMessage").orElse(null);

        // Apply the configuration directly - no need for builder pattern
        if (showHeader) {
            component.setHeaderVisible(true);
            if (headerTitle != null) {
                component.setHeaderTitle(headerTitle);
            }
        }

        if (welcomeMessage != null) {
            component.addWelcomeMessage(welcomeMessage);
        }

        // Note: The view must still provide the MessageProcessor via setMessageProcessor()
    }
}