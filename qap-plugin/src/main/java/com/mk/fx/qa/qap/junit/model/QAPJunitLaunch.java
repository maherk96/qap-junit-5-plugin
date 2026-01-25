package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class QAPJunitLaunch {

  private final QAPHeader header;

  @JsonProperty("testClasses")
  private final List<QAPTestClass> testClasses;

  @JsonCreator
  public QAPJunitLaunch(
      @JsonProperty("header") QAPHeader header,
      @JsonProperty("testClasses") List<QAPTestClass> testClasses) {
    this.header = header;
    this.testClasses = testClasses != null ? testClasses : new ArrayList<>();
  }

  @Override
  public String toString() {
    return "QAPLaunch{" + "header=" + header + ", testClasses=" + testClasses + '}';
  }
}
