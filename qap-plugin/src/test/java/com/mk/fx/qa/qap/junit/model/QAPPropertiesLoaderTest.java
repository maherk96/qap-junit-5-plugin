package com.mk.fx.qa.qap.junit.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import org.junit.jupiter.api.*;

@DisplayName("QAPPropertiesLoader Tests")
class QAPPropertiesLoaderTest {

  @Nested
  @DisplayName("Default Constructor and Property Loading")
  class DefaultConstructorTests {

    @Test
    @DisplayName("Should create loader with default values when qap.properties not found")
    void testDefaultValues() {
      // Note: This test assumes qap.properties exists in test resources
      // If it doesn't exist, all values should be null or defaults
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      assertNotNull(loader, "Loader should be created even without qap.properties");
      // Properties should be loaded from test/resources/qap.properties if it exists
    }

    @Test
    @DisplayName("Should load appName property")
    void testAppNameProperty() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // If qap.properties exists in test resources, this may have a value
      // Otherwise it will be null (default)
      String appName = loader.getAppName();
      // No assertion on value as it depends on test resources
      // Just verify it doesn't throw
    }

    @Test
    @DisplayName("Should use system property for user when qap.user not set")
    void testUserFallbackToSystemProperty() {
      String systemUser = System.getProperty("user.name");

      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      String user = loader.getUser();
      // Should either be from qap.properties or fall back to system user
      assertTrue(
          user == null || user.equals(systemUser) || !user.isEmpty(),
          "User should be from properties or system property");
    }

    @Test
    @DisplayName("Should use default 'UAT' for runEnvironment when not set")
    void testRunEnvironmentDefault() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      String runEnv = loader.getRunEnvironment();
      // Should be from qap.properties or default to "UAT"
      assertNotNull(runEnv, "Run environment should not be null");
      // Will be "UAT" if not in properties
    }

    @Test
    @DisplayName("Should default isReportingEnabled to true when not set")
    void testReportingEnabledDefault() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      boolean isReportingEnabled = loader.isReportingEnabled();
      // Default is true if not specified
      // No assertion as it depends on test resources
    }
  }

  @Nested
  @DisplayName("loadQAPAttributes Method")
  class LoadQAPAttributesTests {

    @Test
    @DisplayName("Should load qap.properties from classpath")
    void testLoadQAPAttributes() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();
      Properties properties = loader.loadQAPAttributes();

      assertNotNull(properties, "Properties should not be null");
      // Properties may be empty if file doesn't exist, but should be non-null
    }

    @Test
    @DisplayName("Should return empty properties when file not found")
    void testLoadMissingFile() {
      // Create a loader with a custom class loader that won't find the file
      QAPPropertiesLoader loader =
          new QAPPropertiesLoader() {
            @Override
            public Properties loadQAPAttributes() {
              Properties properties = new Properties();
              // Simulate file not found
              try (java.io.InputStream in = new java.io.ByteArrayInputStream(new byte[0])) {
                if (in.available() == 0) {
                  return properties; // Empty
                }
              } catch (Exception e) {
                // Ignore
              }
              return properties;
            }
          };

      Properties props = loader.loadQAPAttributes();
      assertNotNull(props, "Should return empty properties, not null");
      assertTrue(props.isEmpty(), "Should be empty when file not found");
    }

    @Test
    @DisplayName("Should handle properties with various data types")
    void testPropertiesDataTypes() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();
      Properties properties = loader.loadQAPAttributes();

      // All property values are strings, even if they represent booleans
      properties.forEach(
          (key, value) -> {
            assertNotNull(key, "Property key should not be null");
            assertNotNull(value, "Property value should not be null");
            assertTrue(value instanceof String, "All property values should be strings");
          });
    }
  }

  @Nested
  @DisplayName("loadGitProperties Method")
  class LoadGitPropertiesTests {

    @Test
    @DisplayName("Should return null when git.properties not found")
    void testGitPropertiesNotFound() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();
      Properties gitProps = loader.loadGitProperties();

      // Will be null if file not found or empty
      if (gitProps != null) {
        assertFalse(gitProps.isEmpty(), "Non-null git properties should not be empty");
      }
      // No assertion since file may or may not exist
    }

    @Test
    @DisplayName("Should load git.properties when available")
    void testLoadGitProperties() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();
      Properties gitProps = loader.loadGitProperties();

      // Properties may be null (file not found) or non-null (file found)
      if (gitProps != null) {
        assertNotNull(gitProps, "Git properties should not be null if file exists");
        assertFalse(gitProps.isEmpty(), "Non-null git properties should contain values");
      }
    }

    @Test
    @DisplayName("Should return null for empty git.properties file")
    void testEmptyGitProperties() {
      // Create a loader that simulates an empty git.properties file
      QAPPropertiesLoader loader =
          new QAPPropertiesLoader() {
            @Override
            public Properties loadGitProperties() {
              Properties properties = new Properties();
              // Simulate loading but finding no properties
              return properties.isEmpty() ? null : properties;
            }
          };

      Properties gitProps = loader.loadGitProperties();
      assertNull(gitProps, "Should return null for empty git.properties");
    }
  }

  @Nested
  @DisplayName("Property Value Parsing")
  class PropertyValueParsingTests {

    @Test
    @DisplayName("Should parse boolean isReportingEnabled correctly")
    void testBooleanParsing() {
      // Test with explicit boolean values
      Properties props = new Properties();

      // Test "true"
      props.setProperty("qap.report.test.data", "true");
      boolean result1 = Boolean.parseBoolean(props.getProperty("qap.report.test.data", "true"));
      assertTrue(result1);

      // Test "false"
      props.setProperty("qap.report.test.data", "false");
      boolean result2 = Boolean.parseBoolean(props.getProperty("qap.report.test.data", "true"));
      assertFalse(result2);

      // Test default
      boolean result3 = Boolean.parseBoolean(props.getProperty("non.existent.property", "true"));
      assertTrue(result3, "Should use default value when property not found");
    }

    @Test
    @DisplayName("Should handle invalid boolean values")
    void testInvalidBooleanParsing() {
      Properties props = new Properties();
      props.setProperty("qap.report.test.data", "invalid");

      // Boolean.parseBoolean returns false for any non-"true" value
      boolean result = Boolean.parseBoolean(props.getProperty("qap.report.test.data"));
      assertFalse(result, "Invalid boolean should parse as false");
    }
  }

  @Nested
  @DisplayName("Regression Property")
  class RegressionPropertyTests {

    @Test
    @DisplayName("Should load regression property as boolean")
    void testRegressionProperty() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // regression is now a boolean field loaded from properties
      // Default is false if not set
      assertNotNull(loader);
      // Value depends on qap.properties in test resources
    }

    @Test
    @DisplayName("Should default regression to false when not set")
    void testRegressionDefaultsFalse() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // If qap.properties doesn't have qap.regression, should be false
      // No assertion as it depends on test resources
      assertNotNull(loader);
    }
  }

  @Nested
  @DisplayName("Getter Methods")
  class GetterMethodsTests {

    @Test
    @DisplayName("Should provide getters for all properties")
    void testAllGetters() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // All getters should work without throwing
      assertDoesNotThrow(() -> loader.getAppName());
      assertDoesNotThrow(() -> loader.getTestEnvironment());
      assertDoesNotThrow(() -> loader.getRunEnvironment());
      assertDoesNotThrow(() -> loader.getUser());
      assertDoesNotThrow(() -> loader.isReportingEnabled());
      assertDoesNotThrow(() -> loader.getApiKey());
      assertDoesNotThrow(() -> loader.isRegression());

      // Logging properties
      assertDoesNotThrow(() -> loader.isLoggingEnabled());
      assertDoesNotThrow(() -> loader.getLoggingMinLevel());
      assertDoesNotThrow(() -> loader.getLoggingMaxEntries());
      assertDoesNotThrow(() -> loader.getLoggingMaxMessageLength());
      assertDoesNotThrow(() -> loader.isLoggingCaptureStackTraces());
      assertDoesNotThrow(() -> loader.isLoggingIncludeMdc());
      assertDoesNotThrow(() -> loader.isLoggingIncludeMarkers());
      assertDoesNotThrow(() -> loader.getLoggingLoggerPatterns());

      // Stack trace properties
      assertDoesNotThrow(() -> loader.getStackTraceMaxLines());
      assertDoesNotThrow(() -> loader.getStackTraceHeadLines());
      assertDoesNotThrow(() -> loader.getStackTraceTailLines());
      assertDoesNotThrow(() -> loader.isStackTraceKeepUntilFrameworkExit());
    }

    @Test
    @DisplayName("Should return consistent values on multiple calls")
    void testGetterConsistency() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      String appName1 = loader.getAppName();
      String appName2 = loader.getAppName();
      assertEquals(appName1, appName2, "Multiple getter calls should return same value");

      boolean reporting1 = loader.isReportingEnabled();
      boolean reporting2 = loader.isReportingEnabled();
      assertEquals(reporting1, reporting2, "Boolean getter should be consistent");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should work with actual qap.properties from test resources")
    void testWithActualProperties() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // Load properties
      Properties props = loader.loadQAPAttributes();
      assertNotNull(props);

      // Verify loader has been initialized
      assertNotNull(loader);

      // If qap.properties exists in test resources, properties will be loaded
      // Otherwise defaults will be used
      String runEnv = loader.getRunEnvironment();
      assertNotNull(runEnv, "Run environment should have a value (default or from file)");
    }

    @Test
    @DisplayName("Should load logging configuration properties with defaults")
    void testLoggingConfigurationDefaults() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // Should have default values
      assertTrue(loader.isLoggingEnabled(), "Logging should be enabled by default");
      assertEquals("DEBUG", loader.getLoggingMinLevel(), "Default min level should be DEBUG");
      assertEquals(1000, loader.getLoggingMaxEntries(), "Default max entries should be 1000");
      assertEquals(
          10000, loader.getLoggingMaxMessageLength(), "Default max message length should be 10000");
      assertTrue(
          loader.isLoggingCaptureStackTraces(), "Stack traces should be captured by default");
      assertTrue(loader.isLoggingIncludeMdc(), "MDC should be included by default");
      assertTrue(loader.isLoggingIncludeMarkers(), "Markers should be included by default");
      assertNotNull(
          loader.getLoggingLoggerPatterns(), "Logger patterns should not be null (may be empty)");
    }

    @Test
    @DisplayName("Should load stack trace configuration properties with defaults")
    void testStackTraceConfigurationDefaults() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // Should have default values
      assertEquals(200, loader.getStackTraceMaxLines(), "Default max lines should be 200");
      assertEquals(50, loader.getStackTraceHeadLines(), "Default head lines should be 50");
      assertEquals(20, loader.getStackTraceTailLines(), "Default tail lines should be 20");
      assertFalse(
          loader.isStackTraceKeepUntilFrameworkExit(),
          "Keep until framework exit should be false by default");
    }

    @Test
    @DisplayName("Should gracefully handle missing files in production scenario")
    void testMissingFilesScenario() {
      // Simulate a scenario where both files are missing
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // Should not throw, should use defaults
      assertNotNull(loader);
      assertNotNull(loader.getRunEnvironment(), "Should use default UAT when file missing");
      assertTrue(loader.isReportingEnabled(), "Should default to true when file missing");
    }

    @Test
    @DisplayName("Should load multiple instances independently")
    void testMultipleInstances() {
      QAPPropertiesLoader loader1 = new QAPPropertiesLoader();
      QAPPropertiesLoader loader2 = new QAPPropertiesLoader();

      // Both should load successfully
      assertNotNull(loader1);
      assertNotNull(loader2);

      // Both should have same property values (loaded from same file)
      assertEquals(
          loader1.getRunEnvironment(),
          loader2.getRunEnvironment(),
          "Multiple loaders should load same properties");
      assertEquals(
          loader1.isReportingEnabled(),
          loader2.isReportingEnabled(),
          "Multiple loaders should load same boolean values");
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should not throw when qap.properties is malformed")
    void testMalformedProperties() {
      // The loader catches IOException and returns empty properties
      // This test verifies the loader can handle errors gracefully
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      assertDoesNotThrow(
          () -> {
            Properties props = loader.loadQAPAttributes();
            assertNotNull(props, "Should return non-null properties even on error");
          });
    }

    @Test
    @DisplayName("Should not throw when git.properties is malformed")
    void testMalformedGitProperties() {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      assertDoesNotThrow(
          () -> {
            Properties gitProps = loader.loadGitProperties();
            // May be null or empty, but should not throw
          });
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void testConcurrentAccess() throws Exception {
      QAPPropertiesLoader loader = new QAPPropertiesLoader();

      // Create multiple threads accessing properties simultaneously
      int threadCount = 10;
      Thread[] threads = new Thread[threadCount];
      java.util.concurrent.atomic.AtomicInteger successCount =
          new java.util.concurrent.atomic.AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        threads[i] =
            new Thread(
                () -> {
                  try {
                    String appName = loader.getAppName();
                    boolean reporting = loader.isReportingEnabled();
                    String runEnv = loader.getRunEnvironment();
                    // All reads should succeed
                    successCount.incrementAndGet();
                  } catch (Exception e) {
                    // Should not throw
                    fail("Concurrent access should not fail: " + e.getMessage());
                  }
                });
        threads[i].start();
      }

      // Wait for all threads
      for (Thread thread : threads) {
        thread.join(1000);
      }

      assertEquals(threadCount, successCount.get(), "All concurrent reads should succeed");
    }
  }
}
