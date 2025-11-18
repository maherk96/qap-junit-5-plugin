package com.mk.fx.qa.qap.junit.model;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
public abstract class QAPBaseTestCase {

    protected long startTime;
    protected long endTime;
    protected String status;

    @ToString.Exclude
    @JsonIgnore
    protected byte[] logs;

    @ToString.Exclude
    @JsonIgnore
    protected byte[] fix;

    @ToString.Exclude
    @JsonIgnore
    protected byte[] exception;

    protected Set<String> tag = new HashSet<>();
    protected Set<String> classTags = new HashSet<>();
    protected Set<String> inheritedClassTags = new HashSet<>();

    @JsonIgnore
    public Set<String> getMethodTags() {
        return Collections.unmodifiableSet(tag);
    }

    @JsonIgnore
    public Set<String> getTag() {
        return Collections.unmodifiableSet(tag);
    }

    @JsonIgnore
    public Set<String> getInheritedClassTags() {
        return Collections.unmodifiableSet(inheritedClassTags);
    }

    public void setInheritedClassTags(Set<String> tags) {
        this.inheritedClassTags.clear();
        if (tags != null) {
            this.inheritedClassTags.addAll(tags);
        }
    }

    public void addInheritedClassTags(Set<String> tags) {
        if (tags != null) {
            this.inheritedClassTags.addAll(tags);
        }
    }

    @JsonIgnore
    public Set<String> getClassTags() {
        return Collections.unmodifiableSet(classTags);
    }

    @JsonProperty("tags")
    public QAPTags getTags() {
        return new QAPTags(
                Collections.unmodifiableSet(tag),
                Collections.unmodifiableSet(classTags),
                Collections.unmodifiableSet(inheritedClassTags)
        );
    }

    public void setClassTags(Set<String> tags) {
        this.classTags.clear();
        if (tags != null) {
            this.classTags.addAll(tags);
        }
    }

    public void addTag(String tag) {
        this.tag.add(tag);
    }

    public boolean hasTags() {
        return isNotEmpty(tag);
    }

    public boolean hasLogs() {
        return isNotEmpty(logs);
    }

    // Always serialize logs as an array for consistency
    @JsonProperty("logs")
    public java.util.List<String> getLogs() {
        return java.util.Collections.emptyList();
    }

    @JsonProperty("durationMillis")
    public long getDurationMillis() {
        return (endTime > 0L && startTime > 0L && endTime >= startTime) ? (endTime - startTime) : 0L;
    }

    public boolean hasFix() {
        return isNotEmpty(fix);
    }

    public boolean hasException() {
        return isNotEmpty(exception);
    }

    // Serialize fix/exception as arrays when absent to avoid nulls
    @JsonProperty("fix")
    public java.util.List<String> getFixArray() {
        return java.util.Collections.emptyList();
    }

    @JsonProperty("exception")
    public java.util.List<String> getExceptionArray() {
        return java.util.Collections.emptyList();
    }
}
