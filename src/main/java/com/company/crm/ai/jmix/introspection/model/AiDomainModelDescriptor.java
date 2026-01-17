package com.company.crm.ai.jmix.introspection.model;

import java.util.Map;

/**
 * AI-optimized domain model containing all entities.
 * Root container for domain model export to AI systems.
 */
public record AiDomainModelDescriptor(Map<String, AiEntityDescriptor> entities) {
}