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
        // Apply XML configuration directly to the component
        boolean showHeader = loadBoolean(element, "showHeader").orElse(false);

        // Load resource strings for internationalization
        loadResourceString(element, "headerTitle", context.getMessageGroup(), component::setHeaderTitle);
        loadResourceString(element, "welcomeMessage", context.getMessageGroup(), component::setWelcomeMessage);
        loadResourceString(element, "assistantName", context.getMessageGroup(), component::setAssistantName);
        loadResourceString(element, "userName", context.getMessageGroup(), component::setUserName);

        // Apply the configuration directly - no need for builder pattern
        if (showHeader) {
            component.setHeaderVisible(true);
        }

        // Note: The view must still provide the MessageProcessor via setMessageProcessor()
    }
}