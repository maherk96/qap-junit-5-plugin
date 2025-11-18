package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class QAPTestClass {

    private final String className;
    private final String displayName;
    private List<QAPJunitLifeCycleEvent> qapJunitLifeCycleEvent;
    private final Set<String> classTags;
    private Set<String> inheritedClassTags;
    private String classKey;
    private List<String> classChain;
    private List<QAPTest> testCases;

    @JsonCreator
    public QAPTestClass(
            @JsonProperty("className") String className,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("classTags") Set<String> classTags) {
        this.className = className;
        this.displayName = displayName;
        this.classTags = classTags;
        this.qapJunitLifeCycleEvent = new ArrayList<>();
    }

    @JsonProperty("tags")
    public QAPClassTags getTags() {
        return new QAPClassTags(classTags, inheritedClassTags);
    }

    @JsonIgnore
    public Set<String> getInheritedClassTags() {
        return inheritedClassTags;
    }
}
