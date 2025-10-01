package com.mk.fx.qa.qap.junit.model;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Data
public class QAPPropertiesLoader {

    private static final Logger log = LoggerFactory.getLogger(QAPPropertiesLoader.class);

    private final String appName;
    private final String fixMessageLogging;
    private final String testEnvironment;
    private final String runEnvironment;
    private final String user;
    private final boolean isReportingEnabled;
    private final String apiKey;

    private String isRegression;

    public QAPPropertiesLoader() {
        Properties qapAttributes = loadQAPAttributes();
        this.appName = qapAttributes.getProperty("qap.app.name");
        this.fixMessageLogging = qapAttributes.getProperty("qap.report.fix.messaging");
        this.user = qapAttributes.getProperty("qap.user", System.getProperty("user.name"));
        this.testEnvironment = qapAttributes.getProperty("qap.test.environment");
        this.runEnvironment = qapAttributes.getProperty("qap.run.environment", "UAT");
        this.isReportingEnabled =
                Boolean.parseBoolean(qapAttributes.getProperty("qap.report.test.data", "true"));
        this.apiKey = qapAttributes.getProperty("qap.api.key");
    }

    public Properties loadQAPAttributes() {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("qap.properties")) {
            if (in == null) {
                throw new IOException("Unable to find qap.properties");
            }
            properties.load(in);
        } catch (IOException e) {
            log.error("Unable to load properties: {}", e.getMessage());
        }
        return properties;
    }

    public Properties loadGitProperties() {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("git.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            log.error("Unable to load git.properties: {}", e.getMessage());
            return new Properties();
        }
        return properties.isEmpty() ? null : properties;
    }
}