package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QAPTest extends QAPBaseTestCase {

    private final String methodName;
    private final String displayName;
    @ToString.Exclude
    private byte[] testParams;

    @JsonCreator
    public QAPTest(
            @JsonProperty("methodName") String methodName,
            @JsonProperty("displayName") String displayName) {
        this.methodName = methodName;
        this.displayName = displayName;
    }

    public boolean hasTestParams() {
        return isNotEmpty(testParams);
    }
}