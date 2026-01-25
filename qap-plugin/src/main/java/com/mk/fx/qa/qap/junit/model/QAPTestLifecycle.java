package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Represents the test-case-level lifecycle execution, including per-test fixtures and test
 * execution.
 *
 * <p>This does NOT include class-level fixtures (@BeforeAll/@AfterAll) - those are tracked at the
 * test class level.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPTestLifecycle {

  private List<QAPTestFixture> beforeEach = new ArrayList<>();
  private TestExecution test;
  private List<QAPTestFixture> afterEach = new ArrayList<>();

  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TestExecution {
    @JsonProperty("durationNanos")
    private Long durationNanos;

    private List<com.mk.fx.qa.qap.logging.core.QAPLogEntry>
        logEntries; // Logs captured during test execution only (excludes fixtures)
  }
}
