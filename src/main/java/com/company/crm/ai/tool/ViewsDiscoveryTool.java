package com.company.crm.ai.tool;

import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.view.ViewInfo;
import io.jmix.flowui.view.ViewRegistry;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.List;
import java.util.Objects;

public class ViewsDiscoveryTool implements CrmAiTool {

    private final ServerProperties serverProperties;
    private final ViewRegistry viewRegistry;
    private final MetadataTools metadataTools;

    public static ViewsDiscoveryTool create(ApplicationContext applicationContext) {
        return new ViewsDiscoveryTool(
                applicationContext.getBean(ServerProperties.class),
                applicationContext.getBean(ViewRegistry.class),
                applicationContext.getBean(MetadataTools.class));
    }

    public ViewsDiscoveryTool(ServerProperties serverProperties,
                              ViewRegistry viewRegistry,
                              MetadataTools metadataTools) {
        this.serverProperties = serverProperties;
        this.viewRegistry = viewRegistry;
        this.metadataTools = metadataTools;
    }

    @Tool(description = """
            Get the complete list of all available view routes in the system.
            
            Use this to:
            - Explore the available view routes
            """)
    public List<String> getAvailableRoutes() {
        return viewRegistry.getViewInfos()
                .stream()
                .map(this::getRoute)
                .filter(Objects::nonNull)
                .toList();
    }

    @Tool(description = """
            Get the route for the list view of a given entity.
            
            Use this to:
            - Create link to the list view of a specific entity
            """)
    @Nullable
    public String getEntityListViewRoute(
            @ToolParam(description = "Name of the entity for which to retrieve the list view route")
            String entityName) {
        return getEntityViewRoute(entityName, viewRegistry::getListViewId);
    }

    @Tool(description = """
            Get the route for the detail (edit) view of a given entity.
            
            Use this to:
            - Create link to the detail (edit) view of a specific entity
            """)
    @Nullable
    public String getEntityDetailViewRoute(
            @ToolParam(description = "Name of the entity for which to retrieve the detail (edit) view route")
            String entityName) {
        return getEntityViewRoute(entityName, viewRegistry::getDetailViewId);
    }

    @Nullable
    private String getEntityViewRoute(String entityName, java.util.function.Function<io.jmix.core.metamodel.model.MetaClass, String> viewIdProvider) {
        return metadataTools.getAllJpaEntityMetaClasses().stream()
                .filter(metaClass -> Objects.equals(metaClass.getName(), entityName))
                .findFirst()
                .flatMap(metaClass -> viewRegistry.findViewInfo(viewIdProvider.apply(metaClass))
                        .map(this::getRoute))
                .orElse(null);
    }

    @Tool(description = """
            Generate a relative link to the list view of a given entity.
            
            Use this to:
            - Create link to the list view of a specific entity
            """)
    @Nullable
    public String generateEntityListLink(
            @ToolParam(description = "Name of the entity for which to generate the list view link")
            String entityName
    ) {
        String viewRoute = getEntityListViewRoute(entityName);
        if (StringUtils.isEmpty(viewRoute)) {
            return null;
        }
        return "%s/%s".formatted(getServletContextPath(), viewRoute);
    }

    @Tool(description = """
            Generate a relative link to the detail (edit) view of a given entity.
            
            Use this to:
            - Create link to the detail (edit) view of a specific entity
            """)
    @Nullable
    public String generateEntityDetailLink(
            @ToolParam(description = "Name of the entity for which to generate the detail link")
            String entityName,
            @ToolParam(description = "ID of the entity for which to generate the detail link")
            String entityId) {
        String viewRoute = getEntityDetailViewRoute(entityName);
        viewRoute = StringUtils.substringBeforeLast(viewRoute, "/");

        if (StringUtils.isEmpty(viewRoute)) {
            return null;
        }

        return "%s/%s/%s".formatted(getServletContextPath(), viewRoute, entityId);
    }

    @Tool(description = """
            Get the context path of the application.
            
            Use this to:
            - Get the context path of the application
            """)
    public String getServletContextPath() {
        String contextPath = serverProperties.getServlet().getContextPath();
        return contextPath == null ? "" : contextPath;
    }

    @Nullable
    private String getRoute(ViewInfo viewInfo) {
        Route routeAnnotation = AnnotationUtils.findAnnotation(viewInfo.getControllerClass(), Route.class);
        if (routeAnnotation != null) {
            return routeAnnotation.value();
        } else {
            return null;
        }
    }
}
