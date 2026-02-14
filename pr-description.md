# Jmix CRM AI Integration

This PR integrates Spring AI into the CRM sample, implementing persistent chat memory, reusable UI components, and domain model introspection for natural language data analysis.

![Architecture Overview](https://github.com/user-attachments/assets/your-image-id-here)

## Generic Parts

Since you mentioned wanting to build an add-on for this, I wanted to highlight these components that could potentially go into a Jmix AI add-on.

### Spring AI Memory Implementation

I went with the standard Jmix approach here - using **[AiConversation](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/entity/AiConversation.java)** and **[ChatMessage](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/entity/ChatMessage.java)** entities with the **[JmixChatMemoryRepository](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/jmix/memory/JmixChatMemoryRepository.java)** implementing Spring AI's `ChatMemoryRepository` interface. Instead of Spring AI's JDBC implementations, this uses `DataManager` and standard Jmix patterns.

**Discussion:** For a real add-on we'd need to think about tenant support, soft deletion etc. on these entities.

### UI Components

The **[AiConversationComponent](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/view/component/aiconversation/AiConversationComponent.java)** is implemented as a Vaadin Composite with `MessageList` and `MessageInput` components. It supports `@InstallSubject("messageProcessor")` for declarative configuration. I built a **[custom component loader](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/view/component/aiconversation/AiConversationComponentLoader.java)** and **[XSD schema](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/resources/com/company/crm/view/component/aiconversation/ai-conversation-component.xsd)** to support XML descriptor configuration. Background processing is handled by the **[async processor](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/view/component/aiconversation/AiConversationComponentAsyncMessageProcessor.java)**.

**Discussion:** I built this as a Composite component because I had the potential add-on in mind, but I'm not happy with how the implementation turned out. The architecture with the async processor and component loader doesn't feel like it's add-on-ready yet. Since the component is always used with `AiConversation` entities anyway, my suggestion would be to simplify this for the demo - maybe go with a Fragment approach for now. When we actually implement the add-on, we can think through better solutions for the component architecture.

### Entity Model Introspection

The **[JmixJpaEntityDiscoveryTool](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/jmix/introspection/JmixJpaEntityDiscoveryTool.java)** enumerates all available entities using Jmix `MetadataTools`. Deep entity analysis is performed by **[JpaDomainModelIntrospector](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/jmix/introspection/introspector/JpaDomainModelIntrospector.java)** which uses specialized property introspectors to analyze data properties, JPA relationships, embedded objects, and enums. The model can be exported as structured YAML through the **[exporter](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/jmix/introspection/AiDomainModelDescriptorYamlExporter.java)**.

**Discussion:** This is primarily built to support the JPQL query execution tool - instead of putting the entire domain model structure in the system prompt every time, the AI can discover it dynamically. However, we now have a third way to export domain models (REST module JSON, planned visualization features, and this YAML approach). The YAML format includes AI-specific enhancements like `@Comment` annotation extraction for field descriptions and enum value translations that the standard metadata API doesn't provide, but which are crucial for proper LLM context.

### Dynamic JPQL Query Execution

AI-generated queries are executed through **[JpqlQueryTool](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/jmix/query/JpqlQueryTool.java)** which provides the Spring AI `@Tool` interface. **[AiJpqlParameterConverter](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/jmix/query/AiJpqlParameterConverter.java)** handles automatic parameter type conversion including date string parsing and numeric conversion to `BigDecimal`. Query results are processed by **[ResultConverter](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/ai/jmix/query/ResultConverter.java)** for AI consumption.

**Discussion:** I built this iteratively by asking the LLM various questions and then updating the system prompt/tool descriptions based on what queries it generated and where exceptions occurred. The goal was to get the success rate high enough that most queries work on the first try (around 80-90%). This approach has some inherent challenges: JPQL is already more limited than SQL, and Jmix's JPQL support is a subset of the standard. LLMs don't know these limitations out of the box, so I put the constraints in the tool descriptions.

I knew DataManager's property filters wouldn't be sufficient - when you ask complex business questions like "What are the payment behavior patterns of our top 10 clients, and which ones have concerning overdue invoice trends?", you quickly need joins and aggregations that would be just as complex to express in a custom DSL. The JPQL approach works but has two limitations: 1) we're constrained by Jmix's JPQL subset, and 2) we're giving full read access to the database at the JPA level, which could cause performance issues on large datasets if the AI generates queries without proper indexes.

## CRM-Specific Implementation

This demonstrates how the generic AI infrastructure gets applied to a specific business domain.

### CRM Analytics Integration

The **[CrmAnalyticsService](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/app/service/ai/CrmAnalyticsService.java)** ties everything together using Spring AI's `ChatClient` with function calling capabilities. The **[system prompt](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/resources/prompts/crm-analytics-system-prompt.st)** configures the AI as a CRM Business Analytics Expert with specific JPQL security constraints and interactive entity linking patterns. Async response handling is provided by **[CrmAnalyticsAsyncLoader](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/app/service/ai/CrmAnalyticsAsyncLoader.java)** while the **[MainView](https://github.com/jmix-framework/jmix-crm/blob/ai/src/main/java/com/company/crm/view/main/MainView.java)** integration provides the floating AI chat access throughout the application.

### Row-Level Security Gap

The current example has Manager, Supervisor, and Administrator roles, but there's no row-level security implemented yet. This is actually crucial for the JPQL Query Tool because it's what differentiates us from generic solutions like Postgres MCP servers. We should be able to demonstrate that queries go through the Jmix layer (not directly to the database) and that a user only sees their own data - for example, sales reps should only see clients from their region, or only products they're authorized to sell. The CRM example needs to be extended with different users having different data access scopes to properly showcase this capability.