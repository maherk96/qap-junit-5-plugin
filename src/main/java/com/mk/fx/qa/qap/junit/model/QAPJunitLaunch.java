package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class QAPJunitLaunch {

    private final QAPHeader header;
    private final QAPTestClass testClass;

    @JsonCreator
    public QAPJunitLaunch(
            @JsonProperty("header") QAPHeader header,
            @JsonProperty("testClass") QAPTestClass testClass) {
        this.header = header;
        this.testClass = testClass;
    }

    @Override
    public String toString() {
        return "QAPLaunch{" +
                "header=" + header +
                ", testClass=" + testClass +
                '}';
    }
}