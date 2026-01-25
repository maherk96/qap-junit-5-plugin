package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Represents a class-level fixture execution (@BeforeAll, @AfterAll).
 *
 * <p>These fixtures run once per test class, not per test case. They are stored at the test class
 * level in the fixtures list.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPFixture {

  private final String phase; // BEFORE_ALL, AFTER_ALL
  private final String methodName; // e.g. "setupClass"
  private final String
      className; // Fully qualified: e.g. "com.example.testapp.PaymentProcessorTest"
  private final String status; // PASSED, FAILED, ABORTED
  private final Long durationNanos;
  private final QAPFailure failure; // error if fixture failed (renamed from failure in JSON)
  private final java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry>
      logEntries; // Only included when non-empty

  @JsonCreator
  public QAPFixture(
      @JsonProperty("phase") String phase,
      @JsonProperty("methodName") String methodName,
      @JsonProperty("className") String className,
      @JsonProperty("status") String status,
      @JsonProperty("durationNanos") Long durationNanos,
      @JsonProperty("error") QAPFailure failure,
      @JsonProperty("logEntries")
          java.util.List<com.mk.fx.qa.qap.logging.core.QAPLogEntry> logEntries) {
    this.phase = phase;
    this.methodName = methodName;
    this.className = className;
    this.status = status;
    this.durationNanos = durationNanos;
    this.failure = failure;
    this.logEntries = logEntries;
  }

  /**
   * Convenience constructor for creating fixtures without log entries (backwards compatibility).
   */
  public QAPFixture(
      String phase, String status, Long durationMillis, Long durationNanos, QAPFailure failure) {
    this(phase, null, null, status, durationNanos, failure, null);
  }

  /** Serialize failure as "error" in JSON to match test fixture terminology. */
  @JsonProperty("error")
  public QAPFailure getError() {
    return failure;
  }

  /** Hide the original failure getter from JSON serialization. */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public QAPFailure getFailure() {
    return failure;
  }
}
