package com.mk.fx.qa.qap.junit.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    this.user =
        qapAttributes.getProperty(
            "qap.user", System.getProperty(com.mk.fx.qa.qap.junit.core.SystemProperties.USER_NAME));
    this.testEnvironment = qapAttributes.getProperty("qap.test.environment");
    this.runEnvironment = qapAttributes.getProperty("qap.run.environment", "UAT");
    this.isReportingEnabled =
        Boolean.parseBoolean(qapAttributes.getProperty("qap.report.test.data", "true"));
    this.apiKey = qapAttributes.getProperty("qap.api.key");
  }

  /**
   * Loads QAP configuration properties from classpath. If qap.properties is not found, returns
   * empty Properties with a warning. This allows tests to run with default values.
   *
   * @return Properties object (may be empty if file not found or parsing failed)
   */
  public Properties loadQAPAttributes() {
    Properties properties = new Properties();
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("qap.properties")) {
      if (in == null) {
        log.warn("qap.properties not found on classpath, using default configuration values");
        return properties; // Return empty properties to use defaults
      }
      properties.load(in);
      log.debug("Successfully loaded {} properties from qap.properties", properties.size());
    } catch (IOException e) {
      log.error("Failed to parse qap.properties: {}", e.getMessage(), e);
      // Return empty properties rather than propagating exception
      // This allows tests to continue with default values
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
