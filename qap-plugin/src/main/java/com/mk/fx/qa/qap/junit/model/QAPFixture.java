package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/** Represents a lifecycle fixture execution (BeforeAll, BeforeEach, AfterEach, AfterAll). */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPFixture {

  private final String phase; // BEFORE_ALL, BEFORE_EACH, AFTER_EACH, AFTER_ALL
  private final String status; // PASSED, FAILED, ABORTED
  private final Long durationMillis;
  private final Long durationNanos;
  private final QAPFailure failure;

  @JsonCreator
  public QAPFixture(
      @JsonProperty("phase") String phase,
      @JsonProperty("status") String status,
      @JsonProperty("durationMillis") Long durationMillis,
      @JsonProperty("durationNanos") Long durationNanos,
      @JsonProperty("failure") QAPFailure failure) {
    this.phase = phase;
    this.status = status;
    this.durationMillis = durationMillis;
    this.durationNanos = durationNanos;
    this.failure = failure;
  }
}
