package com.mk.fx.qa.qap.junit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String parentDisplayName;      // e.g. "Second Level Nested Context"
    private String parentClassKey;         // e.g. "NestedTestsExample$FirstNested"
    private List<String> parentChain;      // e.g. ["Top-Level Test Class", "First Nested Context", "Second Level Nested Context"]

    private List<QAPTestParams> parameters;

    @JsonCreator
    public QAPTest(@JsonProperty("methodName") String methodName,
                   @JsonProperty("displayName") String displayName,
                   @JsonProperty("methodDisplayName") String methodDisplayName,
                   @JsonProperty("parentDisplayName") String parentDisplayName,
                   @JsonProperty("parentClassKey") String parentClassKey,
                   @JsonProperty("parentChain") List<String> parentChain) {
        this.methodName = methodName;
        this.displayName = displayName;
        // Initialize new fields with safe defaults for null-safety
        this.methodDisplayName = methodDisplayName != null ? methodDisplayName : "";
        this.parentDisplayName = parentDisplayName != null ? parentDisplayName : "";
        this.parentClassKey = parentClassKey != null ? parentClassKey : "";
        this.parentChain = parentChain != null ? parentChain : new ArrayList<>();
    }

    // Backwards-compatible convenience constructor used by tests and call sites
    public QAPTest(String methodName, String displayName) {
        this(methodName, displayName, null, null, null, null);
    }

    public boolean hasParameters() {
        return parameters != null && !parameters.isEmpty();
    }
}
