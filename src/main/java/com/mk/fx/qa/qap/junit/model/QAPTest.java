package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QAPTest extends QAPBaseTestCase {

    private final String methodName;       // e.g. "parameterizedTest"
    private final String displayName;      // e.g. "Run 1 with value=A"
    private String testCaseId;             // e.g. "DemoExtensionUsageTest#parameterizedTest[0]"
    private String methodDisplayName;      // e.g. "Parameterized test in SecondLevelNested"

    private List<QAPTestParams> parameters;
    private String testType; // TEST, PARAMETERIZED, etc.

    @com.fasterxml.jackson.annotation.JsonProperty("parameters")
    public java.util.List<QAPTestParams> getParametersOrEmpty() {
        return parameters != null ? parameters : java.util.Collections.emptyList();
    }

    // Backwards-compatible convenience constructor used by tests and call sites
    public QAPTest(String methodName, String displayName) {
        this.methodName = methodName;
        this.displayName = displayName;
    }

    public boolean hasParameters() {
        return parameters != null && !parameters.isEmpty();
    }
}
