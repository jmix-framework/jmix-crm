package com.company.crm.app.online;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.CurrentAuthentication;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

/**
 * Routing datasource that delegates to datasources created for each HTTP session.
 * <p>
 * For proper removal of datasources of removed and expired sessions requires
 * <pre>
 *     jmix.ui.use-session-fixation-protection = false
 * </pre>
 */
public class RoutingDataSource extends AbstractDataSource implements ApplicationContextAware, VaadinServiceInitListener {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

    protected Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    protected ApplicationContext applicationContext;

    protected String urlPrefix;
    protected String defaultSessionId;
    protected String sessionDataSourceBeanName;

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getDefaultSessionId() {
        return defaultSessionId;
    }

    public void setDefaultSessionId(String defaultSessionId) {
        this.defaultSessionId = defaultSessionId;
    }

    public String getSessionDataSourceBeanName() {
        return sessionDataSourceBeanName;
    }

    public void setSessionDataSourceBeanName(String sessionDataSourceBeanName) {
        this.sessionDataSourceBeanName = sessionDataSourceBeanName;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return determineSessionDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineSessionDataSource().getConnection(username, password);
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionDestroyListener(this::onSessionDestroyed);
    }

    protected DataSource determineSessionDataSource() {
        String sessionId = getSessionId();
        log.debug("Session datasource {} is used", sessionId);
        return dataSources.computeIfAbsent(sessionId, this::createSessionDataSource);
    }

    protected String getSessionId() {
        CurrentAuthentication currentAuthentication = applicationContext.getBean(CurrentAuthentication.class);

        String sessionId = null;
        if (currentAuthentication.isSet()) {
            Authentication authentication = currentAuthentication.getAuthentication();
            Object details = authentication.getDetails();

            if (details instanceof WebAuthenticationDetails) {
                sessionId = ((WebAuthenticationDetails) details).getSessionId();
            } else if (details instanceof ClientDetails) {
                sessionId = ((ClientDetails) details).getSessionId();
            }
        }

        return sessionId != null ? sessionId : defaultSessionId;
    }

    protected DataSource createSessionDataSource(String sessionId) {
        log.info("Creating datasource for session {}", sessionId);
        BasicDataSource sessionDataSource = (BasicDataSource) applicationContext.getBean(sessionDataSourceBeanName);
        sessionDataSource.setUrl(urlPrefix + sessionId);

        LiquibaseProperties liquibaseProperties = applicationContext.getBean(LiquibaseProperties.class);
        liquibaseProperties.setEnabled(true);
        applicationContext.getBean(SpringLiquibase.class, sessionDataSource, liquibaseProperties);

        return sessionDataSource;
    }

    protected void onSessionDestroyed(SessionDestroyEvent event) {
        String sessionId = event.getSession().getSession().getId();

        DataSource sessionDataSource = dataSources.get(sessionId);
        if (sessionDataSource != null) {
            shutdownSessionDataSource(sessionId, sessionDataSource);
            dataSources.remove(sessionId);
        }
    }

    protected void shutdownSessionDataSource(String sessionId, DataSource sessionDataSource) {
        log.info("Removing datasource for session {}", sessionId);
        try (Statement statement = sessionDataSource.getConnection().createStatement()) {
            statement.executeUpdate("SHUTDOWN");
        } catch (SQLException e) {
            log.warn("Error shutting down datasource {}", sessionId);
        }

        try {
            ((BasicDataSource) sessionDataSource).close();
        } catch (SQLException e) {
            log.warn("Error closing datasource {}", sessionId);
        }
    }
}
