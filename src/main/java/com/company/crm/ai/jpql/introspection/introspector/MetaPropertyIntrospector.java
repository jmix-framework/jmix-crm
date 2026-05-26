package com.company.crm.ai.jpql.introspection.introspector;

import com.company.crm.ai.jpql.introspection.model.AiPropertyDescriptor;
import io.jmix.core.metamodel.model.MetaProperty;

/**
 * Interface for introspecting MetaProperty objects to AI-optimized property descriptors.
 */
public interface MetaPropertyIntrospector {

    /**
     * Introspects a MetaProperty to an AI-optimized property descriptor.
     *
     * @param property the MetaProperty to introspect
     * @return AiPropertyDescriptor representation of this property, null if this introspector cannot handle it
     */
    AiPropertyDescriptor introspect(MetaProperty property);

    /**
     * Checks if this introspector can handle the given MetaProperty type.
     *
     * @param property the MetaProperty to check
     * @return true if this introspector can handle this property type
     */
    boolean supports(MetaProperty property);

    /**
     * Helper method to get the localized property caption.
     *
     * @param property the MetaProperty
     * @param messageTools Jmix MessageTools bean
     * @return the localized caption of the property
     */
    default String getPropertyCaption(MetaProperty property, io.jmix.core.MessageTools messageTools) {
        return messageTools.getPropertyCaption(property.getDomain(), property.getName());
    }
}