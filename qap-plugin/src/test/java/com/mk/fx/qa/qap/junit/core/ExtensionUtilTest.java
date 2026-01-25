package com.mk.fx.qa.qap.junit.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@DisplayName("ExtensionUtil Tests")
class ExtensionUtilTest {

  @Nested
  @DisplayName("OS Version")
  class OsVersionTests {

    @Test
    @DisplayName("Should return OS version string")
    void testGetOsVersion() {
      String osVersion = ExtensionUtil.getOsVersion();

      assertNotNull(osVersion, "OS version should not be null");
      assertFalse(osVersion.trim().isEmpty(), "OS version should not be empty");
      assertTrue(osVersion.contains(" "), "OS version should contain name and version");
    }

    @Test
    @DisplayName("Should match system properties")
    void testOsVersionMatchesSystemProperties() {
      String osName = System.getProperty("os.name");
      String osVer = System.getProperty("os.version");
      String expected = osName + " " + osVer;

      String actual = ExtensionUtil.getOsVersion();

      assertEquals(expected, actual, "Should combine os.name and os.version");
    }
  }

  @Nested
  @DisplayName("JDK Version")
  class JdkVersionTests {

    @Test
    @DisplayName("Should return JDK version string")
    void testGetJdkVersion() {
      String jdkVersion = ExtensionUtil.getJdkVersion();

      assertNotNull(jdkVersion, "JDK version should not be null");
      assertFalse(jdkVersion.trim().isEmpty(), "JDK version should not be empty");
      assertTrue(jdkVersion.startsWith("JDK "), "Should start with 'JDK '");
    }

    @Test
    @DisplayName("Should match system properties")
    void testJdkVersionMatchesSystemProperties() {
      String javaVersion = System.getProperty("java.version");
      String expected = "JDK " + javaVersion;

      String actual = ExtensionUtil.getJdkVersion();

      assertEquals(expected, actual, "Should be 'JDK ' + java.version");
    }

    @Test
    @DisplayName("Should contain version numbers")
    void testJdkVersionContainsNumbers() {
      String jdkVersion = ExtensionUtil.getJdkVersion();

      assertTrue(jdkVersion.matches(".*\\d+.*"), "JDK version should contain numbers");
    }
  }

  @Nested
  @DisplayName("Regression Enabled Check")
  class RegressionEnabledTests {

    @BeforeEach
    void setUp() {
      System.clearProperty("qap.regression");
    }

    @AfterEach
    void tearDown() {
      System.clearProperty("qap.regression");
    }

    @Test
    @DisplayName("Should return false when qap.regression not set")
    void testRegressionNotSet() {
      System.clearProperty("qap.regression");

      boolean result = ExtensionUtil.isRegressionEnabled();

      assertFalse(result, "Should return false when property not set");
    }

    @Test
    @DisplayName("Should return true when qap.regression is set to true")
    void testRegressionSetToTrue() {
      System.setProperty("qap.regression", "true");

      boolean result = ExtensionUtil.isRegressionEnabled();

      assertTrue(result, "Should return true when property is set");
    }

    @Test
    @DisplayName("Should return true when qap.regression is set to false")
    void testRegressionSetToFalse() {
      System.setProperty("qap.regression", "false");

      boolean result = ExtensionUtil.isRegressionEnabled();

      assertTrue(result, "Should return true when property is set (any value)");
    }

    @Test
    @DisplayName("Should return true when qap.regression is set to empty string")
    void testRegressionSetToEmpty() {
      System.setProperty("qap.regression", "");

      boolean result = ExtensionUtil.isRegressionEnabled();

      assertTrue(result, "Should return true when property is set to empty string");
    }

    @Test
    @DisplayName("Should check property existence, not value")
    void testPropertyExistenceCheck() {
      System.setProperty("qap.regression", "any-value");

      boolean result = ExtensionUtil.isRegressionEnabled();

      assertTrue(result, "Should return true for any non-null value");
    }
  }

  @Nested
  @DisplayName("Utility Class Structure")
  class UtilityClassStructureTests {

    @Test
    @DisplayName("Should not be instantiable")
    void testPrivateConstructor() throws Exception {
      java.lang.reflect.Constructor<ExtensionUtil> constructor =
          ExtensionUtil.class.getDeclaredConstructor();

      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");

      // Attempt to make it accessible and instantiate should work (for coverage)
      constructor.setAccessible(true);
      assertDoesNotThrow(() -> constructor.newInstance());
    }

    @Test
    @DisplayName("All methods should be static")
    void testAllMethodsStatic() {
      java.lang.reflect.Method[] methods = ExtensionUtil.class.getDeclaredMethods();

      for (java.lang.reflect.Method method : methods) {
        if (method.isSynthetic() || method.getName().contains("$")) {
          continue; // Skip synthetic methods
        }
        assertTrue(
            java.lang.reflect.Modifier.isStatic(method.getModifiers()),
            "Method " + method.getName() + " should be static");
      }
    }
  }
}
