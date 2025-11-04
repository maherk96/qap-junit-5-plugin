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
    protected byte[] logs;

    @ToString.Exclude 
    protected byte[] fix;

    @ToString.Exclude 
    protected byte[] exception;

    protected Set<String> tag = new HashSet<>();
    protected Set<String> inheritedClassTags = new HashSet<>();

    // Keep internal field name `tag`, but expose as `methodTags` in JSON
    @JsonProperty("methodTags")
    public Set<String> getMethodTags() {
        return Collections.unmodifiableSet(tag);
    }

    // Avoid emitting legacy `tag` property in JSON
    @JsonIgnore
    public Set<String> getTag() {
        return Collections.unmodifiableSet(tag);
    }

    @JsonProperty("inheritedClassTags")
    public Set<String> getInheritedClassTags() {
        return Collections.unmodifiableSet(inheritedClassTags);
    }

    public void setInheritedClassTags(Set<String> tags) {
        this.inheritedClassTags.clear();
        if (tags != null) {
            this.inheritedClassTags.addAll(tags);
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

    public boolean hasFix() {
        return isNotEmpty(fix);
    }

    public boolean hasException() {
        return isNotEmpty(exception);
    }
}
