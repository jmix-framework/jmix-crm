package com.company.crm.view.component.aiconversation;

import io.jmix.flowui.sys.registration.ComponentRegistration;
import io.jmix.flowui.sys.registration.ComponentRegistrationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for registering the AiConversationComponent
 * for XML usage in view descriptors.
 */
@Configuration
public class AiConversationComponentRegistration {

    @Bean
    public ComponentRegistration aiConversation() {
        return ComponentRegistrationBuilder.create(AiConversationComponent.class)
                .withComponentLoader("aiConversation", AiConversationComponentLoader.class)
                .build();
    }
}