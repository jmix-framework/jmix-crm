package com.company.crm.ai.jmix.introspection.model;

import java.util.Map;

/**
 * AI-optimized entity descriptor representing a single entity.
 * Contains optional comment, caption and unified property descriptors.
 */
public record AiEntityDescriptor(String caption, String comment,
                                 Map<String, AiPropertyDescriptor> properties) {
}