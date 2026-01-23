package com.mk.fx.qa.qap.junit.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mk.fx.qa.qap.junit.model.QAPHeader;
import com.mk.fx.qa.qap.junit.model.QAPJunitLaunch;
import com.mk.fx.qa.qap.junit.model.QAPPropertiesLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("QAPUtils Tests")
class QAPUtilsTest {

  @Nested
  @DisplayName("Constant Values")
  class ConstantValuesTests {

    @Test
    @DisplayName("Should define TEST_CLASS_DATA_KEY constant")
    void testTestClassDataKey() {
      assertEquals("testClassData", QAPUtils.TEST_CLASS_DATA_KEY);
    }

    @Test
    @DisplayName("Should define METHOD_DESCRIPTION_KEY constant")
    void testMethodDescriptionKey() {
      assertEquals("methodDescription", QAPUtils.METHOD_DESCRIPTION_KEY);
    }

    @Test
    @DisplayName("Should define PARAM_INDEX_KEY constant")
    void testParamIndexKey() {
      assertEquals("paramIndexCounter", QAPUtils.PARAM_INDEX_KEY);
    }

    @Test
    @DisplayName("Should define CLASS_NODES_KEY constant")
    void testClassNodesKey() {
      assertEquals("classNodes", QAPUtils.CLASS_NODES_KEY);
    }

    @Test
    @DisplayName("Should define FIXTURE_START_TIME_KEY constant")
    void testFixtureStartTimeKey() {
      assertEquals("fixtureStartTime", QAPUtils.FIXTURE_START_TIME_KEY);
    }

    @Test
    @DisplayName("Should define FIXTURE_START_TIME_NANOS_KEY constant")
    void testFixtureStartTimeNanosKey() {
      assertEquals("fixtureStartTimeNanos", QAPUtils.FIXTURE_START_TIME_NANOS_KEY);
    }
  }

  @Nested
  @DisplayName("JUnit Version")
  class JunitVersionTests {

    @Test
    @DisplayName("Should return JUnit version string")
    void testGetJunitVersion() {
      String version = QAPUtils.getJunitVersion();

      assertNotNull(version, "JUnit version should not be null");
      assertTrue(version.startsWith("Junit "), "Should start with 'Junit '");
    }

    @Test
    @DisplayName("Should return consistent version")
    void testJunitVersionConsistency() {
      String version1 = QAPUtils.getJunitVersion();
      String version2 = QAPUtils.getJunitVersion();

      assertEquals(version1, version2, "Should return same version on multiple calls");
    }
  }

  @Nested
  @DisplayName("Build QAP Headers")
  class BuildQAPHeadersTests {

    private QAPHeader header;
    private QAPPropertiesLoader propertiesLoader;

    @BeforeEach
    void setUp() {
      header = new QAPHeader(System.currentTimeMillis(), "test-launch-id");
      propertiesLoader = mock(QAPPropertiesLoader.class);

      when(propertiesLoader.getAppName()).thenReturn("TestApp");
      when(propertiesLoader.getTestEnvironment()).thenReturn("UAT");
      when(propertiesLoader.getUser()).thenReturn("testuser");

      System.clearProperty("qap.regression");
    }

    @AfterEach
    void tearDown() {
      System.clearProperty("qap.regression");
    }

    @Test
    @DisplayName("Should populate header with all values")
    void testBuildHeaders() {
      String gitInfo = "main-branch";

      QAPUtils.buildQAPHeaders(header, gitInfo, propertiesLoader);

      assertNotNull(header.getLaunchEndTime(), "Launch end time should be set");
      assertEquals("TestApp", header.getApplicationName());
      assertEquals("UAT", header.getTestEnvironment());
      assertEquals("main-branch", header.getGitBranch());
      assertEquals("testuser", header.getUser());
      assertNotNull(header.getTestRunnerVersion());
      assertNotNull(header.getOsVersion());
      assertNotNull(header.getJdkVersion());
    }

    @Test
    @DisplayName("Should set launch end time to current timestamp")
    void testLaunchEndTime() {
      long beforeTime = System.currentTimeMillis();

      QAPUtils.buildQAPHeaders(header, "git-info", propertiesLoader);

      long afterTime = System.currentTimeMillis();
      long endTime = header.getLaunchEndTime();

      assertTrue(
          endTime >= beforeTime && endTime <= afterTime,
          "End time should be between before and after timestamps");
    }

    @Test
    @DisplayName("Should set regression to false when not enabled")
    void testRegressionFalse() {
      System.clearProperty("qap.regression");

      QAPUtils.buildQAPHeaders(header, "git-info", propertiesLoader);

      assertFalse(header.isRegression(), "Regression should be false when not set");
    }

    @Test
    @DisplayName("Should set regression to true when enabled")
    void testRegressionTrue() {
      System.setProperty("qap.regression", "true");

      QAPUtils.buildQAPHeaders(header, "git-info", propertiesLoader);

      assertTrue(header.isRegression(), "Regression should be true when property is set");
    }

    @Test
    @DisplayName("Should populate test runner version")
    void testTestRunnerVersion() {
      QAPUtils.buildQAPHeaders(header, "git-info", propertiesLoader);

      String testRunnerVersion = header.getTestRunnerVersion();
      assertNotNull(testRunnerVersion);
      assertTrue(testRunnerVersion.startsWith("Junit "));
    }

    @Test
    @DisplayName("Should populate OS version")
    void testOsVersion() {
      QAPUtils.buildQAPHeaders(header, "git-info", propertiesLoader);

      String osVersion = header.getOsVersion();
      assertNotNull(osVersion);
      assertFalse(osVersion.trim().isEmpty());
    }

    @Test
    @DisplayName("Should populate JDK version")
    void testJdkVersion() {
      QAPUtils.buildQAPHeaders(header, "git-info", propertiesLoader);

      String jdkVersion = header.getJdkVersion();
      assertNotNull(jdkVersion);
      assertTrue(jdkVersion.startsWith("JDK "));
    }

    @Test
    @DisplayName("Should handle null git info")
    void testNullGitInfo() {
      QAPUtils.buildQAPHeaders(header, null, propertiesLoader);

      assertNull(header.getGitBranch(), "Git branch should be null when git info is null");
    }

    @Test
    @DisplayName("Should handle empty strings in properties")
    void testEmptyProperties() {
      when(propertiesLoader.getAppName()).thenReturn("");
      when(propertiesLoader.getTestEnvironment()).thenReturn("");
      when(propertiesLoader.getUser()).thenReturn("");

      QAPUtils.buildQAPHeaders(header, "git-info", propertiesLoader);

      assertEquals("", header.getApplicationName());
      assertEquals("", header.getTestEnvironment());
      assertEquals("", header.getUser());
      // Other fields should still be populated
      assertNotNull(header.getLaunchEndTime());
      assertNotNull(header.getTestRunnerVersion());
    }
  }

  @Nested
  @DisplayName("Is Reporting Enabled")
  class IsReportingEnabledTests {

    @Test
    @DisplayName("Should return true when reporting enabled")
    void testReportingEnabled() {
      QAPHeader testHeader = new QAPHeader(System.currentTimeMillis(), "test-id");
      QAPJunitLaunch launch = new QAPJunitLaunch(testHeader, new java.util.ArrayList<>());
      QAPPropertiesLoader loader = mock(QAPPropertiesLoader.class);
      when(loader.isReportingEnabled()).thenReturn(true);

      boolean result = QAPUtils.isReportingEnabled(launch, loader);

      assertTrue(result);
      verify(loader).isReportingEnabled();
    }

    @Test
    @DisplayName("Should return false when reporting disabled")
    void testReportingDisabled() {
      QAPHeader testHeader = new QAPHeader(System.currentTimeMillis(), "test-id");
      QAPJunitLaunch launch = new QAPJunitLaunch(testHeader, new java.util.ArrayList<>());
      QAPPropertiesLoader loader = mock(QAPPropertiesLoader.class);
      when(loader.isReportingEnabled()).thenReturn(false);

      boolean result = QAPUtils.isReportingEnabled(launch, loader);

      assertFalse(result);
      verify(loader).isReportingEnabled();
    }

    @Test
    @DisplayName("Should delegate to properties loader")
    void testDelegation() {
      QAPHeader testHeader = new QAPHeader(System.currentTimeMillis(), "test-id");
      QAPJunitLaunch launch = new QAPJunitLaunch(testHeader, new java.util.ArrayList<>());
      QAPPropertiesLoader loader = mock(QAPPropertiesLoader.class);

      QAPUtils.isReportingEnabled(launch, loader);

      verify(loader, times(1)).isReportingEnabled();
    }
  }

  @Nested
  @DisplayName("Utility Class Structure")
  class UtilityClassStructureTests {

    @Test
    @DisplayName("Should not be instantiable")
    void testPrivateConstructor() throws Exception {
      java.lang.reflect.Constructor<QAPUtils> constructor = QAPUtils.class.getDeclaredConstructor();

      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");

      // Attempt to make it accessible and instantiate (for coverage)
      constructor.setAccessible(true);
      assertDoesNotThrow(() -> constructor.newInstance());
    }

    @Test
    @DisplayName("Should be final class")
    void testFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(QAPUtils.class.getModifiers()),
          "QAPUtils should be final");
    }
  }
}
