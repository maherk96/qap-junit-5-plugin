package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPTestClass {

  private final String className;
  private String displayName; // used for both root and nested classes
  private String fullClassName; // e.g., DemoExtensionUsageTest$MathGroup$InnerGroup
  private final Set<String> classTags;
  private Set<String> inheritedClassTags;

  @JsonProperty("parentClassKey")
  private String classKey;

  @JsonProperty("parentChain")
  private List<String> classChain;

  private List<QAPTest> testCases;
  private List<QAPTestClass> children = new ArrayList<>();

  @JsonCreator
  public QAPTestClass(
      @JsonProperty("className") String className,
      @JsonProperty("displayName") String displayName,
      @JsonProperty("classTags") Set<String> classTags) {
    this.className = className;
    this.displayName = displayName;
    this.classTags = classTags;
  }

  @JsonProperty("tags")
  public QAPClassTags getTags() {
    return new QAPClassTags(classTags, inheritedClassTags);
  }

  @JsonIgnore
  public Set<String> getInheritedClassTags() {
    return inheritedClassTags;
  }

  // Prevent duplicate exposure of classTags alongside the unified tags object
  @JsonIgnore
  public Set<String> getClassTags() {
    return classTags;
  }
}
