package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QAPTest extends QAPBaseTestCase {

    private final String methodName;       // e.g. "parameterizedTest"
    private final String displayName;      // e.g. "Run 1 with value=A"
    private String methodDisplayName;      // e.g. "Parameterized test in SecondLevelNested"
    private String parentDisplayName;      // e.g. "Second Level Nested Context"
    private String parentClassKey;         // e.g. "NestedTestsExample$FirstNested"
    private List<String> parentChain;      // e.g. ["Top-Level Test Class", "First Nested Context", "Second Level Nested Context"]

    @ToString.Exclude
    private byte[] testParams;

    @JsonCreator
    public QAPTest(@JsonProperty("methodName") String methodName,
                   @JsonProperty("displayName") String displayName) {
        this.methodName = methodName;
        this.displayName = displayName;
    }

    public boolean hasTestParams() {
        return isNotEmpty(testParams);
    }
}