package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QAPTestParams(
    @JsonProperty("index") int argumentIndex,
    @JsonProperty("name") String argumentName,
    @JsonProperty("type") String argumentType,
    @JsonProperty("value") String argumentValue) {

  @JsonCreator
  public QAPTestParams {
    // Compact constructor for record
  }
}
