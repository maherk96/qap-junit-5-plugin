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
  private final String testEnvironment;
  private final String runEnvironment;
  private final String user;
  private final boolean isReportingEnabled;
  private final String apiKey;

  private String isRegression;

  // Logging configuration properties
  private final Properties qapProperties;

  public QAPPropertiesLoader() {
    Properties qapAttributes = loadQAPAttributes();
    this.qapProperties = qapAttributes; // Store for later access
    this.appName = qapAttributes.getProperty("qap.app.name");
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
   * Gets a property value with a default.
   *
   * @param key property key
   * @param defaultValue default value if not found
   * @return property value or default
   */
  public String getProperty(String key, String defaultValue) {
    return qapProperties.getProperty(key, defaultValue);
  }

  /**
   * Gets a boolean property value with a default.
   *
   * @param key property key
   * @param defaultValue default value if not found
   * @return property value or default
   */
  public boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = qapProperties.getProperty(key);
    return value != null ? Boolean.parseBoolean(value) : defaultValue;
  }

  /**
   * Gets an integer property value with a default.
   *
   * @param key property key
   * @param defaultValue default value if not found or parsing fails
   * @return property value or default
   */
  public int getIntProperty(String key, int defaultValue) {
    String value = qapProperties.getProperty(key);
    if (value != null) {
      try {
        return Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        log.warn("Invalid integer value for {}: '{}', using default: {}", key, value, defaultValue);
      }
    }
    return defaultValue;
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
