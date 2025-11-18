package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Set;

/**
 * Tags container at the class level.
 * Serialized as: "tags": { "class": [...], "inherited": [...] }
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class QAPClassTags {

    @JsonProperty("class")
    private final Set<String> clazz;

    @JsonProperty("inherited")
    private final Set<String> inherited;

    @JsonCreator
    public QAPClassTags(
            @JsonProperty("class") Set<String> clazz,
            @JsonProperty("inherited") Set<String> inherited) {
        this.clazz = clazz != null ? clazz : Collections.emptySet();
        this.inherited = inherited != null ? inherited : Collections.emptySet();
    }

    public Set<String> getClazz() {
        return clazz;
    }

    public Set<String> getInherited() {
        return inherited;
    }
}

