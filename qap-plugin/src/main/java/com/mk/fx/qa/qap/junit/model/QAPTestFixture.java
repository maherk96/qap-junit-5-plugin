package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Represents a single fixture execution with method and class information. Used within test cases
 * to track which fixtures ran for each test.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPTestFixture {

  private final String methodName; // e.g., "setUp", "setUpAccounts", "tearDown"
  private final String className; // e.g., "BankServiceTest", "TransferTests"
  private final Integer order; // Execution order within the phase (1, 2, 3...)
  private final String status; // PASSED, FAILED, ABORTED
  private final Long durationNanos;
  private final QAPFailure error; // Error if fixture failed

  @JsonCreator
  public QAPTestFixture(
      @JsonProperty("methodName") String methodName,
      @JsonProperty("className") String className,
      @JsonProperty("order") Integer order,
      @JsonProperty("status") String status,
      @JsonProperty("durationNanos") Long durationNanos,
      @JsonProperty("error") QAPFailure error) {
    this.methodName = methodName;
    this.className = className;
    this.order = order;
    this.status = status;
    this.durationNanos = durationNanos;
    this.error = error;
  }
}
