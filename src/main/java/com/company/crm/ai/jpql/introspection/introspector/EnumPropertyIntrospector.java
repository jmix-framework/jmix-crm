package com.company.crm.ai.jpql.introspection.introspector;

import com.company.crm.ai.jpql.introspection.model.AiEnumValueDescriptor;
import com.company.crm.ai.jpql.introspection.model.AiPropertyDescriptor;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
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
        return AiPropertyDescriptor.enumProperty(
                getPropertyCaption(property, messageTools),
                metadataTools.getMetaAnnotationValue(property, Comment.class),
                property.getJavaType().getSimpleName(),
                enumValues(property.getJavaType())
        );
    }

    private Map<String, AiEnumValueDescriptor> enumValues(Class<?> enumClass) {
        Map<String, AiEnumValueDescriptor> enums = new LinkedHashMap<>();
        if (EnumClass.class.isAssignableFrom(enumClass)) {
            for (Object enumConstant : enumClass.getEnumConstants()) {
                EnumClass<?> enumClassConstant = (EnumClass<?>) enumConstant;
                enums.put(enumConstant.toString(), new AiEnumValueDescriptor(
                        enumClassConstant.getId(),
                        getLocalizedDescription(enumConstant)
                ));
            }
        } else {
            for (Object enumConstant : enumClass.getEnumConstants()) {
                enums.put(enumConstant.toString(), new AiEnumValueDescriptor(
                        (((Enum<?>) enumConstant).ordinal()),
                        getLocalizedDescription(enumConstant)
                ));
            }
        }
        return enums;
    }

    private String getLocalizedDescription(Object enumConstant) {
        String enumName = enumConstant.toString();
        String enumDescription = messages.getMessage((Enum<?>) enumConstant);
        return !enumDescription.equals(enumName) ? enumDescription : null;
    }
}
