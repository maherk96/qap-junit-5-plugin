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

  // ========================================
  // Property Name Constants
  // ========================================
  public static final String PROP_APP_NAME = "qap.app.name";
  public static final String PROP_TEST_ENV = "qap.test.environment";
  public static final String PROP_RUN_ENV = "qap.run.environment";
  public static final String PROP_USER = "qap.user";
  public static final String PROP_REPORTING_ENABLED = "qap.report.test.data";
  public static final String PROP_API_KEY = "qap.api.key";
  public static final String PROP_REGRESSION = "qap.regression";

  public static final String PROP_LOGGING_ENABLED = "qap.logging.enabled";
  public static final String PROP_LOGGING_MIN_LEVEL = "qap.logging.min.level";
  public static final String PROP_LOGGING_MAX_ENTRIES = "qap.logging.max.entries";
  public static final String PROP_LOGGING_MAX_MESSAGE_LENGTH = "qap.logging.max.message.length";
  public static final String PROP_LOGGING_CAPTURE_STACKTRACES = "qap.logging.capture.stacktraces";
  public static final String PROP_LOGGING_INCLUDE_MDC = "qap.logging.include.mdc";
  public static final String PROP_LOGGING_INCLUDE_MARKERS = "qap.logging.include.markers";
  public static final String PROP_LOGGING_LOGGER_PATTERNS = "qap.logging.logger.patterns";

  public static final String PROP_STACKTRACE_MAX_LINES = "qap.stacktrace.max.lines";
  public static final String PROP_STACKTRACE_HEAD_LINES = "qap.stacktrace.head.lines";
  public static final String PROP_STACKTRACE_TAIL_LINES = "qap.stacktrace.tail.lines";
  public static final String PROP_STACKTRACE_KEEP_UNTIL_FRAMEWORK_EXIT =
      "qap.stacktrace.keep.until.framework.exit";

  // ========================================
  // Application & Environment Properties
  // ========================================
  private final String appName;
  private final String testEnvironment;
  private final String runEnvironment;
  private final String user;
  private final boolean reportingEnabled;
  private final String apiKey;
  private final boolean regression;

  // ========================================
  // Log Capture Configuration
  // ========================================
  private final boolean loggingEnabled;
  private final String loggingMinLevel;
  private final int loggingMaxEntries;
  private final int loggingMaxMessageLength;
  private final boolean loggingCaptureStackTraces;
  private final boolean loggingIncludeMdc;
  private final boolean loggingIncludeMarkers;
  private final String loggingLoggerPatterns;

  // ========================================
  // Stack Trace Configuration
  // ========================================
  private final int stackTraceMaxLines;
  private final int stackTraceHeadLines;
  private final int stackTraceTailLines;
  private final boolean stackTraceKeepUntilFrameworkExit;

  // Raw properties access for unknown/future properties
  private final Properties qapProperties;

  public QAPPropertiesLoader() {
    Properties props = loadQAPAttributes();
    this.qapProperties = props; // Store for later access

    // ========================================
    // Application & Environment
    // ========================================
    this.appName = props.getProperty(PROP_APP_NAME);
    this.testEnvironment = props.getProperty(PROP_TEST_ENV);
    this.runEnvironment = props.getProperty(PROP_RUN_ENV, "UAT");
    this.user =
        props.getProperty(
            PROP_USER, System.getProperty(com.mk.fx.qa.qap.junit.core.SystemProperties.USER_NAME));
    this.reportingEnabled = parseBoolean(props, PROP_REPORTING_ENABLED, true);
    this.apiKey = props.getProperty(PROP_API_KEY);
    this.regression = parseBoolean(props, PROP_REGRESSION, false);

    // ========================================
    // Log Capture Configuration
    // ========================================
    this.loggingEnabled = parseBoolean(props, PROP_LOGGING_ENABLED, true);
    this.loggingMinLevel = props.getProperty(PROP_LOGGING_MIN_LEVEL, "DEBUG");
    this.loggingMaxEntries = parseInt(props, PROP_LOGGING_MAX_ENTRIES, 1000);
    this.loggingMaxMessageLength = parseInt(props, PROP_LOGGING_MAX_MESSAGE_LENGTH, 10000);
    this.loggingCaptureStackTraces = parseBoolean(props, PROP_LOGGING_CAPTURE_STACKTRACES, true);
    this.loggingIncludeMdc = parseBoolean(props, PROP_LOGGING_INCLUDE_MDC, true);
    this.loggingIncludeMarkers = parseBoolean(props, PROP_LOGGING_INCLUDE_MARKERS, true);
    this.loggingLoggerPatterns = props.getProperty(PROP_LOGGING_LOGGER_PATTERNS, "");

    // ========================================
    // Stack Trace Configuration
    // ========================================
    this.stackTraceMaxLines = parseInt(props, PROP_STACKTRACE_MAX_LINES, 200);
    this.stackTraceHeadLines = parseInt(props, PROP_STACKTRACE_HEAD_LINES, 50);
    this.stackTraceTailLines = parseInt(props, PROP_STACKTRACE_TAIL_LINES, 20);
    this.stackTraceKeepUntilFrameworkExit =
        parseBoolean(props, PROP_STACKTRACE_KEEP_UNTIL_FRAMEWORK_EXIT, false);
  }

  /**
   * Parses a boolean property value with a default.
   *
   * @param props the properties object
   * @param key property key
   * @param defaultValue default value if not found
   * @return property value or default
   */
  private static boolean parseBoolean(Properties props, String key, boolean defaultValue) {
    String value = props.getProperty(key);
    return value != null ? Boolean.parseBoolean(value) : defaultValue;
  }

  /**
   * Parses an integer property value with a default.
   *
   * @param props the properties object
   * @param key property key
   * @param defaultValue default value if not found or parsing fails
   * @return property value or default
   */
  private static int parseInt(Properties props, String key, int defaultValue) {
    String value = props.getProperty(key);
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
   * Gets a property value with a default. Use this for accessing properties not pre-loaded in the
   * constructor.
   *
   * @param key property key
   * @param defaultValue default value if not found
   * @return property value or default
   */
  public String getProperty(String key, String defaultValue) {
    return qapProperties.getProperty(key, defaultValue);
  }

  /**˚
   * Gets a boolean property value with a default. Use this for accessing properties not pre-loaded
   * in the constructor.
   *
   * @param key property key
   * @param defaultValue default value if not found
   * @return property value or default
   */
  public boolean getBooleanProperty(String key, boolean defaultValue) {
    return parseBoolean(qapProperties, key, defaultValue);
  }

  /**
   * Gets an integer property value with a default. Use this for accessing properties not pre-loaded
   * in the constructor.
   *
   * @param key property key
   * @param defaultValue default value if not found or parsing fails
   * @return property value or default
   */
  public int getIntProperty(String key, int defaultValue) {
    return parseInt(qapProperties, key, defaultValue);
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
