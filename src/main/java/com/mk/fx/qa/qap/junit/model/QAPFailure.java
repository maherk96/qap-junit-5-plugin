package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Structured failure information for a test case.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPFailure {

  private final String type;
  private final String message;
  @JsonIgnore private final String stackTrace; // Internal storage
  private final QAPFailure causedBy;
  private final List<QAPFailure> suppressed;
  private final QAPFailureLocation location;
  private final QAPRootCause rootCause; // Root cause from exception chain

  @JsonCreator
  public QAPFailure(
      @JsonProperty("type") String type,
      @JsonProperty("message") String message,
      @JsonProperty("stackTrace") String stackTrace,
      @JsonProperty("causedBy") QAPFailure causedBy,
      @JsonProperty("suppressed") List<QAPFailure> suppressed,
      @JsonProperty("location") QAPFailureLocation location,
      @JsonProperty("rootCause") QAPRootCause rootCause) {
    this.type = type;
    this.message = message;
    this.stackTrace = stackTrace;
    this.causedBy = causedBy;
    this.suppressed = suppressed != null ? suppressed : Collections.emptyList();
    this.location = location;
    this.rootCause = rootCause;
  }

  /**
   * Returns the stack trace as an array of lines for proper JSON formatting.
   * This preserves line breaks in the JSON output, making it readable like a JUnit report.
   * Leading tabs are stripped for cleaner output.
   */
  @JsonProperty("stackTrace")
  public List<String> getStackTraceLines() {
    if (stackTrace == null || stackTrace.isEmpty()) {
      return null;
    }
    // Split by line breaks and strip leading tabs/whitespace for cleaner output
    return Arrays.stream(stackTrace.split("\r?\n"))
        .map(line -> line.replaceFirst("^\t+", "")) // Remove leading tabs
        .collect(Collectors.toList());
  }

  public List<QAPFailure> getSuppressed() {
    return suppressed != null ? suppressed : Collections.emptyList();
  }
}
