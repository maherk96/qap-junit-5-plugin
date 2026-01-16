package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Metadata about how a test was parameterized (e.g., CsvSource, MethodSource, etc.).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPParameterization {

  private final String provider; // e.g., "CsvSource", "MethodSource", "ValueSource"
  private final Integer invocationIndex; // 0-based index of this invocation

  @JsonCreator
  public QAPParameterization(
      @JsonProperty("provider") String provider,
      @JsonProperty("invocationIndex") Integer invocationIndex) {
    this.provider = provider;
    this.invocationIndex = invocationIndex;
  }
}
