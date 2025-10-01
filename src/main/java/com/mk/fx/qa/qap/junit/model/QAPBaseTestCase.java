package com.mk.fx.qa.qap.junit.model;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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

    public Set<String> getTag() {
        return Collections.unmodifiableSet(tag);
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