package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/** Location information for a failure, extracted from stack trace. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPFailureLocation {

  private final String clazz;
  private final String method;
  private final String file;
  private final Integer line;

  @JsonCreator
  public QAPFailureLocation(
      @JsonProperty("class") String clazz,
      @JsonProperty("method") String method,
      @JsonProperty("file") String file,
      @JsonProperty("line") Integer line) {
    this.clazz = clazz;
    this.method = method;
    this.file = file;
    this.line = line;
  }

  @JsonProperty("class")
  public String getClazz() {
    return clazz;
  }
}
