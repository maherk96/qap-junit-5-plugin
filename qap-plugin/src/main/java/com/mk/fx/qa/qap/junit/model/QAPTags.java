package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Set;

/**
 * Immutable container for test tags grouped by origin. Serialized as: "tags": { "class": [...],
 * "method": [...], "inherited": [...] } Empty properties are omitted from JSON for clarity.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class QAPTags {

  @JsonProperty("method")
  private final Set<String> method;

  // Cannot use getClass() due to Object#getClass(); map via @JsonProperty
  @JsonProperty("class")
  private final Set<String> clazz;

  @JsonProperty("inherited")
  private final Set<String> inherited;

  @JsonCreator
  public QAPTags(
      @JsonProperty("method") Set<String> method,
      @JsonProperty("class") Set<String> clazz,
      @JsonProperty("inherited") Set<String> inherited) {
    this.method = method != null ? method : Collections.emptySet();
    this.clazz = clazz != null ? clazz : Collections.emptySet();
    this.inherited = inherited != null ? inherited : Collections.emptySet();
  }

  public Set<String> getMethod() {
    return method;
  }

  public Set<String> getClazz() {
    return clazz;
  }

  public Set<String> getInherited() {
    return inherited;
  }
}
