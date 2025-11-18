package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record QAPTestParams(
    @JsonProperty("index") int argumentIndex,
    @JsonProperty("type") String argumentType,
    @JsonProperty("value") String argumentValue) {

  @JsonCreator
  public QAPTestParams {}
}
