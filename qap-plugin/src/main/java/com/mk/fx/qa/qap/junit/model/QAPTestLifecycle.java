package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Represents the complete lifecycle execution for a test case, including all fixtures (beforeAll,
 * beforeEach, afterEach, afterAll) and test execution.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPTestLifecycle {

  private List<QAPTestFixture> beforeAll = new ArrayList<>();
  private List<QAPTestFixture> beforeEach = new ArrayList<>();
  private TestExecution test;
  private List<QAPTestFixture> afterEach = new ArrayList<>();
  private List<QAPTestFixture> afterAll = new ArrayList<>();

  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TestExecution {
    @JsonProperty("durationNanos")
    private Long durationNanos;

    private String status;
  }
}
