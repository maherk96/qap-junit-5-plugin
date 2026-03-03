package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

/**
 * Represents a single test case execution with complete metadata, tags, timing, and lifecycle
 * information.
 *
 * <p>This class captures:
 *
 * <ul>
 *   <li>Test identification (method name, display name, test case ID)
 *   <li>Execution timing (wall-clock time and high-precision nanosecond duration)
 *   <li>Test status and failure details
 *   <li>Tags (method-level, class-level, and inherited)
 *   <li>Parameters for parameterized tests
 *   <li>Complete lifecycle phases (beforeEach, test, afterEach with fixtures)
 *   <li>Log entries captured during execution
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QAPTest {

  // ========================================================================
  // Test Identity
  // ========================================================================

  private final String methodName; // e.g. "parameterizedTest"
  private final String displayName; // e.g. "Run 1 with value=A"
  private String testCaseId; // e.g. "DemoExtensionUsageTest#parameterizedTest[0]"
  private String methodDisplayName; // e.g. "Parameterized test in SecondLevelNested"

  // ========================================================================
  // Timing (from QAPBaseTestCase)
  // ========================================================================

  protected long startTime; // epoch milliseconds for wall-clock ordering
  protected long endTime; // epoch milliseconds for wall-clock ordering
  @JsonIgnore protected long startTimeNanos; // System.nanoTime() for precise duration measurement
  @JsonIgnore protected long endTimeNanos; // System.nanoTime() for precise duration measurement

  // ========================================================================
  // Status & Failure (from QAPBaseTestCase)
  // ========================================================================

  protected String status;
  @JsonIgnore protected QAPFailure failure;

  // ========================================================================
  // Tags (from QAPBaseTestCase)
  // ========================================================================

  protected Set<String> tag = new HashSet<>();
  protected Set<String> classTags = new HashSet<>();
  protected Set<String> inheritedClassTags = new HashSet<>();

  // ========================================================================
  // Coverage Items
  // ========================================================================

  protected List<String> coverageItems;

  // ========================================================================
  // Test-Specific Fields
  // ========================================================================

  private List<QAPTestParams> parameters;
  private QAPParameterization parameterization;
  private String testType; // TEST, PARAMETERIZED, etc.
  private String disabledReason; // Reason why test was disabled (only set when status is DISABLED)
  private QAPTestLifecycle lifecycle; // Complete lifecycle tracking including fixtures

  @JsonIgnore // Hidden - use lifecycle phase logs instead
  private List<com.mk.fx.qa.qap.logging.core.QAPLogEntry>
      logEntries; // Captured log entries during test execution

  // ========================================================================
  // Constructors
  // ========================================================================

  /** Backwards-compatible convenience constructor used by tests and call sites. */
  public QAPTest(String methodName, String displayName) {
    this.methodName = methodName;
    this.displayName = displayName;
  }

  // ========================================================================
  // Tag Methods (from QAPBaseTestCase)
  // ========================================================================

  @JsonIgnore
  public Set<String> getMethodTags() {
    return Collections.unmodifiableSet(tag);
  }

  @JsonIgnore
  public Set<String> getTag() {
    return Collections.unmodifiableSet(tag);
  }

  @JsonIgnore
  public Set<String> getInheritedClassTags() {
    return Collections.unmodifiableSet(inheritedClassTags);
  }

  public void setInheritedClassTags(Set<String> tags) {
    this.inheritedClassTags.clear();
    if (tags != null) {
      this.inheritedClassTags.addAll(tags);
    }
  }

  public void addInheritedClassTags(Set<String> tags) {
    if (tags != null) {
      this.inheritedClassTags.addAll(tags);
    }
  }

  @JsonIgnore
  public Set<String> getClassTags() {
    return Collections.unmodifiableSet(classTags);
  }

  @JsonProperty("tags")
  public QAPTags getTags() {
    return new QAPTags(
        Collections.unmodifiableSet(tag),
        Collections.unmodifiableSet(classTags),
        Collections.unmodifiableSet(inheritedClassTags));
  }

  public void setClassTags(Set<String> tags) {
    this.classTags.clear();
    if (tags != null) {
      this.classTags.addAll(tags);
    }
  }

  public void addTag(String tag) {
    this.tag.add(tag);
  }

  // ========================================================================
  // Coverage Item Methods
  // ========================================================================

  @JsonProperty("coverageItems")
  public List<String> getCoverageItems() {
    return coverageItems != null ? Collections.unmodifiableList(coverageItems) : null;
  }

  public void setCoverageItems(List<String> items) {
    this.coverageItems = items;
  }

  // ========================================================================
  // Duration Methods (from QAPBaseTestCase)
  // ========================================================================

  @JsonIgnore
  public long getDurationMillis() {
    return (endTime > 0L && startTime > 0L && endTime >= startTime) ? (endTime - startTime) : 0L;
  }

  /**
   * Returns the test duration in nanoseconds. If nanosecond timestamps are not available
   * (startTimeNanos/endTimeNanos), falls back to converting millisecond duration.
   *
   * <p>Note: The millisecond fallback provides lower precision (only accurate to milliseconds) but
   * maintains backward compatibility with existing code paths.
   *
   * @return duration in nanoseconds, or 0 if timing data is unavailable
   */
  @JsonIgnore
  public long getDurationNanos() {
    if (endTimeNanos > 0L && startTimeNanos > 0L && endTimeNanos >= startTimeNanos) {
      return endTimeNanos - startTimeNanos;
    }
    // Fallback: convert millisecond duration to nanoseconds
    // Note: This is less precise but maintains backward compatibility
    long millis = getDurationMillis();
    return millis > 0L ? millis * 1_000_000L : 0L;
  }

  // ========================================================================
  // Failure Methods (from QAPBaseTestCase)
  // ========================================================================

  @JsonIgnore
  public boolean hasException() {
    return failure != null;
  }

  @JsonIgnore
  public boolean hasFailure() {
    return failure != null;
  }

  @JsonProperty("failure")
  public QAPFailure getFailure() {
    return failure;
  }

  // ========================================================================
  // Test-Specific Methods
  // ========================================================================

  @JsonProperty("parameters")
  public List<QAPTestParams> getParametersOrEmpty() {
    return parameters != null ? parameters : Collections.emptyList();
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }

  /** Returns the total duration including all fixtures (beforeEach, test, afterEach). */
  @JsonProperty("totalDurationNanos")
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
  @JsonIgnore
  public Long getTestOnlyDurationNanos() {
    if (lifecycle != null
        && lifecycle.getTest() != null
        && lifecycle.getTest().getDurationNanos() != null) {
      return lifecycle.getTest().getDurationNanos();
    }
    return getDurationNanos();
  }
}
