package com.company.crm.ai.jmix.introspection.introspector;

import com.company.crm.ai.jmix.introspection.model.AiPropertyDescriptor;
import io.jmix.core.Messages;
import io.jmix.core.MessageTools;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.datatype.EnumClass;
import io.jmix.core.metamodel.model.MetaProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Introspects ENUM MetaProperties into AiProperty objects.
 */
@Component
public class EnumPropertyIntrospector implements MetaPropertyIntrospector {

    private final MetadataTools metadataTools;
    private final Messages messages;
    private final MessageTools messageTools;

    public EnumPropertyIntrospector(MetadataTools metadataTools, Messages messages, MessageTools messageTools) {
        this.metadataTools = metadataTools;
        this.messages = messages;
        this.messageTools = messageTools;
    }

    @Override
    public boolean supports(MetaProperty property) {
        return property.getType() == MetaProperty.Type.ENUM;
    }

    @Override
    public AiPropertyDescriptor introspect(MetaProperty property) {
        if (!supports(property)) {
            return null;
        }
        String javaType = property.getJavaType().getSimpleName();
        String type = property.getType().name().toLowerCase();
        Boolean identifier = isIdProperty(property) ? true : null;
        String comment = metadataTools.getMetaAnnotationValue(property, Comment.class);
        String caption = getPropertyCaption(property);
        Map<String, Object> enumValues = null;
        Map<String, String> enumDescriptions = null;
        Class<?> enumClass = property.getJavaType();

        if (enumClass.isEnum()) {
            enumValues = new LinkedHashMap<>();
            enumDescriptions = new LinkedHashMap<>();

            if (EnumClass.class.isAssignableFrom(enumClass)) {
                for (Object enumConstant : enumClass.getEnumConstants()) {
                    EnumClass<?> enumClassConstant = (EnumClass<?>) enumConstant;
                    String enumName = enumConstant.toString();
                    enumValues.put(enumName, enumClassConstant.getId());

                    String enumDescription = messages.getMessage((Enum<?>) enumConstant);
                    if (!enumDescription.equals(enumName)) {
                        enumDescriptions.put(enumName, enumDescription);
                    }
                }
            } else {
                for (Object enumConstant : enumClass.getEnumConstants()) {
                    String enumName = enumConstant.toString();
                    enumValues.put(enumName, ((Enum<?>) enumConstant).ordinal());

                    String enumDescription = messages.getMessage((Enum<?>) enumConstant);
                    if (!enumDescription.equals(enumName)) {
                        enumDescriptions.put(enumName, enumDescription);
                    }
                }
            }

            if (enumDescriptions.isEmpty()) {
                enumDescriptions = null;
            }
        }

        return AiPropertyDescriptor.enumProperty(caption, comment, javaType, enumValues, enumDescriptions);
    }

    private boolean isIdProperty(MetaProperty property) {
        if (property.getAnnotatedElement().isAnnotationPresent(jakarta.persistence.Id.class) ||
            property.getAnnotatedElement().isAnnotationPresent(jakarta.persistence.EmbeddedId.class)) {
            return true;
        }

        return "id".equals(property.getName());
    }

    private String getPropertyCaption(MetaProperty property) {
        return messageTools.getPropertyCaption(property.getDomain(), property.getName());
    }
}