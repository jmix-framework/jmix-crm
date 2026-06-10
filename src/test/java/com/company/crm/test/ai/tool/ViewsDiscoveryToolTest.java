package com.company.crm.test.ai.tool;

import com.company.crm.AbstractTest;
import com.company.crm.ai.tool.ViewsDiscoveryTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ViewsDiscoveryToolTest extends AbstractTest {

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void shouldReturnAvailableRoutes() {
        // given
        ViewsDiscoveryTool tool = ViewsDiscoveryTool.create(applicationContext);

        // when
        List<String> routes = tool.getAvailableRoutes();

        // then
        assertThat(routes).isNotEmpty();
        assertThat(routes).contains("clients");
        assertThat(routes).contains("invoices");
        assertThat(routes).contains("contacts/:id");
        assertThat(routes).doesNotContainNull();
    }

    @Test
    void shouldReturnEntityListViewRoute() {
        // given
        ViewsDiscoveryTool tool = ViewsDiscoveryTool.create(applicationContext);

        // when
        String route = tool.getEntityListViewRoute("Client");

        // then
        assertThat(route).isEqualTo("clients");
    }

    @Test
    void shouldReturnEntityDetailViewRoute() {
        // given
        ViewsDiscoveryTool tool = ViewsDiscoveryTool.create(applicationContext);

        // when
        String route = tool.getEntityDetailViewRoute("Client");

        // then
        assertThat(route).isEqualTo("clients/:id");
    }

    @Test
    void shouldGenerateEntityListLink() {
        // given
        ViewsDiscoveryTool tool = ViewsDiscoveryTool.create(applicationContext);
        String contextPath = serverProperties.getServlet().getContextPath();
        String expectedPrefix = (contextPath == null || contextPath.isEmpty()) ? "" : contextPath;

        // when
        String link = tool.generateEntityListLink("Client");

        // then
        assertThat(link).isEqualTo(expectedPrefix + "/clients");
    }

    @Test
    void shouldGenerateEntityDetailLink() {
        // given
        ViewsDiscoveryTool tool = ViewsDiscoveryTool.create(applicationContext);
        String contextPath = serverProperties.getServlet().getContextPath();
        String expectedPrefix = (contextPath == null || contextPath.isEmpty()) ? "" : contextPath;

        // when
        String link = tool.generateEntityDetailLink("Client", "123");

        // then
        assertThat(link).isEqualTo(expectedPrefix + "/clients/123");
    }
    
    @Test
    void shouldReturnNullForUnknownEntity() {
        // given
        ViewsDiscoveryTool tool = ViewsDiscoveryTool.create(applicationContext);

        // when & then
        assertThat(tool.getEntityListViewRoute("Unknown")).isNull();
        assertThat(tool.getEntityDetailViewRoute("Unknown")).isNull();
        assertThat(tool.generateEntityListLink("Unknown")).isNull();
        assertThat(tool.generateEntityDetailLink("Unknown", "123")).isNull();
    }
}
