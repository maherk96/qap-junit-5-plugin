package com.mk.fx.qa.qap.junit.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StackTraceConfig Tests")
class StackTraceConfigTest {

  @Nested
  @DisplayName("Default Configuration")
  class DefaultConfiguration {

    @Test
    @DisplayName("Should create default config with expected values")
    void testDefaultConfig() {
      StackTraceConfig config = StackTraceConfig.defaultConfig();

      assertNotNull(config, "Default config should not be null");
      assertEquals(200, config.getMaxLines(), "Default maxLines should be 200");
      assertEquals(50, config.getHeadLines(), "Default headLines should be 50");
      assertEquals(20, config.getTailLines(), "Default tailLines should be 20");
      assertFalse(
          config.isKeepUntilFrameworkExit(), "Default keepUntilFrameworkExit should be false");
    }
  }

  @Nested
  @DisplayName("Builder Pattern")
  class BuilderPattern {

    @Test
    @DisplayName("Should build config with custom values")
    void testCustomConfig() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(100)
              .headLines(30)
              .tailLines(10)
              .keepUntilFrameworkExit(true)
              .build();

      assertNotNull(config);
      assertEquals(100, config.getMaxLines());
      assertEquals(30, config.getHeadLines());
      assertEquals(10, config.getTailLines());
      assertTrue(config.isKeepUntilFrameworkExit());
    }

    @Test
    @DisplayName("Should build config with minimal values")
    void testMinimalConfig() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(10)
              .headLines(3)
              .tailLines(3)
              .keepUntilFrameworkExit(false)
              .build();

      assertNotNull(config);
      assertEquals(10, config.getMaxLines());
      assertEquals(3, config.getHeadLines());
      assertEquals(3, config.getTailLines());
      assertFalse(config.isKeepUntilFrameworkExit());
    }

    @Test
    @DisplayName("Should support unlimited stack trace with maxLines=-1")
    void testUnlimitedConfig() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(-1) // Unlimited
              .headLines(50)
              .tailLines(20)
              .keepUntilFrameworkExit(false)
              .build();

      assertNotNull(config);
      assertEquals(-1, config.getMaxLines(), "Unlimited should be -1");
    }

    @Test
    @DisplayName("Should support very large limits")
    void testVeryLargeLimits() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(10000)
              .headLines(5000)
              .tailLines(5000)
              .keepUntilFrameworkExit(false)
              .build();

      assertNotNull(config);
      assertEquals(10000, config.getMaxLines());
      assertEquals(5000, config.getHeadLines());
      assertEquals(5000, config.getTailLines());
    }
  }

  @Nested
  @DisplayName("From Properties")
  class FromProperties {

    @Test
    @DisplayName("Should create config from properties loader with all values set")
    void testFromPropertiesWithAllValues() {
      // Create mock properties loader
      QAPPropertiesLoader mockLoader =
          new QAPPropertiesLoader() {
            @Override
            public int getIntProperty(String key, int defaultValue) {
              return switch (key) {
                case "qap.stacktrace.max.lines" -> 150;
                case "qap.stacktrace.head.lines" -> 40;
                case "qap.stacktrace.tail.lines" -> 15;
                default -> defaultValue;
              };
            }

            @Override
            public boolean getBooleanProperty(String key, boolean defaultValue) {
              if ("qap.stacktrace.keep.until.framework.exit".equals(key)) {
                return true;
              }
              return defaultValue;
            }
          };

      StackTraceConfig config = StackTraceConfig.fromProperties(mockLoader);

      assertNotNull(config);
      assertEquals(150, config.getMaxLines());
      assertEquals(40, config.getHeadLines());
      assertEquals(15, config.getTailLines());
      assertTrue(config.isKeepUntilFrameworkExit());
    }

    @Test
    @DisplayName("Should use defaults when properties loader is null")
    void testFromPropertiesWithNullLoader() {
      StackTraceConfig config = StackTraceConfig.fromProperties(null);

      assertNotNull(config, "Should return default config when loader is null");
      assertEquals(200, config.getMaxLines());
      assertEquals(50, config.getHeadLines());
      assertEquals(20, config.getTailLines());
      assertFalse(config.isKeepUntilFrameworkExit());
    }

    @Test
    @DisplayName("Should use defaults when properties are not set")
    void testFromPropertiesWithEmptyProperties() {
      // Properties loader with no custom values (returns defaults)
      QAPPropertiesLoader emptyLoader = new QAPPropertiesLoader();

      StackTraceConfig config = StackTraceConfig.fromProperties(emptyLoader);

      assertNotNull(config);
      // Should use the default values from fromProperties method
      assertEquals(200, config.getMaxLines());
      assertEquals(50, config.getHeadLines());
      assertEquals(20, config.getTailLines());
      assertFalse(config.isKeepUntilFrameworkExit());
    }

    @Test
    @DisplayName("Should handle partial property configuration")
    void testFromPropertiesWithPartialValues() {
      QAPPropertiesLoader partialLoader =
          new QAPPropertiesLoader() {
            @Override
            public int getIntProperty(String key, int defaultValue) {
              if ("qap.stacktrace.max.lines".equals(key)) {
                return 100; // Only this is set
              }
              return defaultValue; // Others use defaults
            }
          };

      StackTraceConfig config = StackTraceConfig.fromProperties(partialLoader);

      assertNotNull(config);
      assertEquals(100, config.getMaxLines(), "Should use custom maxLines");
      assertEquals(50, config.getHeadLines(), "Should use default headLines");
      assertEquals(20, config.getTailLines(), "Should use default tailLines");
      assertFalse(config.isKeepUntilFrameworkExit(), "Should use default framework exit");
    }

    @Test
    @DisplayName("Should support unlimited via properties")
    void testUnlimitedViaProperties() {
      QAPPropertiesLoader unlimitedLoader =
          new QAPPropertiesLoader() {
            @Override
            public int getIntProperty(String key, int defaultValue) {
              if ("qap.stacktrace.max.lines".equals(key)) {
                return -1; // Unlimited
              }
              return defaultValue;
            }
          };

      StackTraceConfig config = StackTraceConfig.fromProperties(unlimitedLoader);

      assertNotNull(config);
      assertEquals(-1, config.getMaxLines(), "Should support unlimited via properties");
    }
  }

  @Nested
  @DisplayName("Configuration Scenarios")
  class ConfigurationScenarios {

    @Test
    @DisplayName("Production configuration")
    void testProductionConfig() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(200)
              .headLines(50)
              .tailLines(20)
              .keepUntilFrameworkExit(false)
              .build();

      assertEquals(200, config.getMaxLines());
      assertEquals(50, config.getHeadLines());
      assertEquals(20, config.getTailLines());
    }

    @Test
    @DisplayName("Compact configuration for high-volume testing")
    void testCompactConfig() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(30)
              .headLines(15)
              .tailLines(10)
              .keepUntilFrameworkExit(false)
              .build();

      assertEquals(30, config.getMaxLines());
      assertEquals(15, config.getHeadLines());
      assertEquals(10, config.getTailLines());
    }

    @Test
    @DisplayName("User-code focused configuration")
    void testUserCodeFocusedConfig() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(100)
              .headLines(50)
              .tailLines(20)
              .keepUntilFrameworkExit(true) // Stop at framework boundary
              .build();

      assertEquals(100, config.getMaxLines());
      assertTrue(config.isKeepUntilFrameworkExit());
    }

    @Test
    @DisplayName("Debug configuration with full traces")
    void testDebugConfig() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(-1) // Unlimited
              .headLines(0)
              .tailLines(0)
              .keepUntilFrameworkExit(false)
              .build();

      assertEquals(-1, config.getMaxLines());
      assertFalse(config.isKeepUntilFrameworkExit());
    }
  }

  @Nested
  @DisplayName("Value Object Properties")
  class ValueObjectProperties {

    @Test
    @DisplayName("Should be immutable (Lombok @Value)")
    void testImmutability() {
      StackTraceConfig config1 =
          StackTraceConfig.builder()
              .maxLines(100)
              .headLines(30)
              .tailLines(10)
              .keepUntilFrameworkExit(true)
              .build();

      StackTraceConfig config2 =
          StackTraceConfig.builder()
              .maxLines(100)
              .headLines(30)
              .tailLines(10)
              .keepUntilFrameworkExit(true)
              .build();

      // Should be equal based on values
      assertEquals(config1, config2, "Configs with same values should be equal");
      assertEquals(config1.hashCode(), config2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("Should have toString for debugging")
    void testToString() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(100)
              .headLines(30)
              .tailLines(10)
              .keepUntilFrameworkExit(true)
              .build();

      String toString = config.toString();
      assertNotNull(toString);
      assertTrue(toString.contains("100"), "toString should contain maxLines");
      assertTrue(toString.contains("30"), "toString should contain headLines");
      assertTrue(toString.contains("10"), "toString should contain tailLines");
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("Should handle zero values")
    void testZeroValues() {
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(0)
              .headLines(0)
              .tailLines(0)
              .keepUntilFrameworkExit(false)
              .build();

      assertEquals(0, config.getMaxLines());
      assertEquals(0, config.getHeadLines());
      assertEquals(0, config.getTailLines());
    }

    @Test
    @DisplayName("Should handle head+tail larger than maxLines")
    void testHeadTailLargerThanMax() {
      // This is valid - formatter should handle it gracefully
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(10)
              .headLines(8)
              .tailLines(8) // 8+8 > 10
              .keepUntilFrameworkExit(false)
              .build();

      assertEquals(10, config.getMaxLines());
      assertEquals(8, config.getHeadLines());
      assertEquals(8, config.getTailLines());
    }

    @Test
    @DisplayName("Should handle negative head/tail values")
    void testNegativeHeadTailValues() {
      // Builder allows this - formatter should handle gracefully
      StackTraceConfig config =
          StackTraceConfig.builder()
              .maxLines(100)
              .headLines(-1)
              .tailLines(-1)
              .keepUntilFrameworkExit(false)
              .build();

      assertEquals(-1, config.getHeadLines());
      assertEquals(-1, config.getTailLines());
    }
  }
}
