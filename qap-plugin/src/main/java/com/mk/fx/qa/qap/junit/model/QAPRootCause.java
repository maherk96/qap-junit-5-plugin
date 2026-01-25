package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/** Root cause information extracted from the exception cause chain. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPRootCause {

  private final String type;
  private final String message;

  @JsonCreator
  public QAPRootCause(@JsonProperty("type") String type, @JsonProperty("message") String message) {
    this.type = type;
    this.message = message;
  }
}
