package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QAPTest extends QAPBaseTestCase {

  private final String methodName; // e.g. "parameterizedTest"
  private final String displayName; // e.g. "Run 1 with value=A"
  private String testCaseId; // e.g. "DemoExtensionUsageTest#parameterizedTest[0]"
  private String methodDisplayName; // e.g. "Parameterized test in SecondLevelNested"

  private List<QAPTestParams> parameters;
  private QAPParameterization parameterization;
  private String testType; // TEST, PARAMETERIZED, etc.
  private String disabledReason; // Reason why test was disabled (only set when status is DISABLED)
  private QAPTestLifecycle lifecycle; // Complete lifecycle tracking including fixtures

  @com.fasterxml.jackson.annotation.JsonIgnore // Hidden - use lifecycle phase logs instead
  private List<com.mk.fx.qa.qap.logging.core.QAPLogEntry>
      logEntries; // Captured log entries during test execution

  @com.fasterxml.jackson.annotation.JsonProperty("parameters")
  public java.util.List<QAPTestParams> getParametersOrEmpty() {
    return parameters != null ? parameters : java.util.Collections.emptyList();
  }

  // Backwards-compatible convenience constructor used by tests and call sites
  public QAPTest(String methodName, String displayName) {
    this.methodName = methodName;
    this.displayName = displayName;
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }

  /** Returns the total duration including all fixtures (beforeEach, test, afterEach). */
  @com.fasterxml.jackson.annotation.JsonProperty("totalDurationNanos")
  public Long getTotalDurationNanos() {
    if (lifecycle == null) {
      return getDurationNanos();
    }
    long total = 0L;
    if (lifecycle.getBeforeEach() != null) {
      for (var fixture : lifecycle.getBeforeEach()) {
        if (fixture.getDurationNanos() != null) {
          total += fixture.getDurationNanos();
        }
      }
    }
    if (lifecycle.getTest() != null && lifecycle.getTest().getDurationNanos() != null) {
      total += lifecycle.getTest().getDurationNanos();
    }
    if (lifecycle.getAfterEach() != null) {
      for (var fixture : lifecycle.getAfterEach()) {
        if (fixture.getDurationNanos() != null) {
          total += fixture.getDurationNanos();
        }
      }
    }
    return total > 0 ? total : getDurationNanos();
  }

  /** Returns only the test execution duration (excluding fixtures). */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public Long getTestOnlyDurationNanos() {
    if (lifecycle != null
        && lifecycle.getTest() != null
        && lifecycle.getTest().getDurationNanos() != null) {
      return lifecycle.getTest().getDurationNanos();
    }
    return getDurationNanos();
  }
}
