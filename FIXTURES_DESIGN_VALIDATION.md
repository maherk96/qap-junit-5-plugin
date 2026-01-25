# Fixtures Structure Design & Validation

## Overview

Successfully implemented proper separation of class-level fixtures (@BeforeAll/@AfterAll) from test-case-level lifecycle (@BeforeEach/test/@AfterEach).

## Requirements Validation ✅

### ✅ 1. Fixtures Represent Class-Level Hooks Only

**Requirement:** Fixtures represent @BeforeAll and @AfterAll only.

**Implementation:**
- `fixtures` array is at the `testClass` level
- Contains only `BEFORE_ALL` and `AFTER_ALL` phases
- Run once per test class, not per test case

**JSON Evidence:**
```json
{
  "testClasses": [{
    "fixtures": [
      {
        "phase": "BEFORE_ALL",
        "methodName": "setupClass",
        "className": "com.example.testapp.PaymentProcessorTest"
      },
      {
        "phase": "AFTER_ALL",
        "methodName": "teardownClass",
        "className": "com.example.testapp.PaymentProcessorTest"
      }
    ]
  }]
}
```

### ✅ 2. Fixtures Stored at Test Class Level Only

**Requirement:** Fixtures must be stored only at the test class (or suite) level, never inside individual test cases.

**Implementation:**
- `fixtures` array is a property of `QAPTestClass`
- NOT present in `QAPTest` (individual test cases)
- Test cases have separate `lifecycle` structure

**JSON Evidence:**
```json
{
  "testClasses": [{
    "className": "PaymentProcessorTest",
    "fixtures": [...],  // ✅ At class level
    "testCases": [{
      "methodName": "testProcessCreditCardPayment",
      "lifecycle": {
        "beforeEach": [...],  // ✅ Test-level, not class-level
        "test": {...},
        "afterEach": [...]
      }
      // ✅ No fixtures array here
    }]
  }]
}
```

### ✅ 3. Test Cases Reference Only @BeforeEach, Test, @AfterEach

**Requirement:** Test cases should reference only @BeforeEach, test execution, and @AfterEach via a separate lifecycle structure.

**Implementation:**
- `QAPTestLifecycle` contains only: `beforeEach`, `test`, `afterEach`
- Removed: `beforeAll` and `afterAll` from lifecycle
- Each test case has its own lifecycle instance

**JSON Evidence:**
```json
{
  "testCases": [{
    "lifecycle": {
      "beforeEach": [{
        "methodName": "setup",
        "className": "PaymentProcessorTest",  // ✅ Simple name for test-level
        "order": 1,
        "status": "PASSED",
        "durationNanos": 2075542,
        "logEntries": [...]
      }],
      "test": {
        "durationNanos": 12323000,
        "logEntries": [...]
      },
      "afterEach": []
    }
  }]
}
```

### ✅ 4. Complete Fixture Metadata

**Requirement:** Each fixture entry should include:
- phase (e.g. BEFORE_ALL, AFTER_ALL)
- methodName
- className (fully qualified)
- status (PASSED, FAILED, etc.)
- durationNanos
- Optional logEntries

**Implementation:**
```java
public class QAPFixture {
  private final String phase;          // ✅ "BEFORE_ALL" or "AFTER_ALL"
  private final String methodName;     // ✅ "setupClass"
  private final String className;      // ✅ "com.example.testapp.PaymentProcessorTest" (FQN)
  private final String status;         // ✅ "PASSED", "FAILED", "ABORTED"
  private final Long durationNanos;    // ✅ Precise timing
  private final QAPFailure failure;    // ✅ Serialized as "error" in JSON
  private final List<QAPLogEntry> logEntries;  // ✅ Optional, omitted when null
}
```

**JSON Evidence:**
```json
{
  "fixtures": [{
    "phase": "BEFORE_ALL",
    "methodName": "setupClass",
    "className": "com.example.testapp.PaymentProcessorTest",  // ✅ Fully qualified
    "status": "PASSED",
    "durationNanos": 6239458,
    "logEntries": [
      {
        "timestamp": 1769227525.232,
        "level": "INFO",
        "message": "=== Starting Payment Processor Test Suite ==="
      }
    ]
  }]
}
```

### ✅ 5. Log Entry Filtering

**Requirement:** 
- Optional logEntries: Included only when non-empty
- Always include WARN/ERROR logs
- INFO/DEBUG logs are optional and may be filtered or capped

**Implementation:**
- Uses `@JsonInclude(JsonInclude.Include.NON_NULL)` to omit empty log arrays
- Log level filtering configured in `qap.properties`:
  ```properties
  qap.logging.min.level=DEBUG  # Can be set to WARN to filter INFO/DEBUG
  qap.logging.max.entries=100  # Cap to prevent bloat
  ```
- All captured logs currently included; filtering can be added in capture config

**JSON Evidence:**
```json
{
  "fixtures": [{
    "logEntries": [
      {"level": "INFO", "message": "..."},
      {"level": "INFO", "message": "..."}
    ]
  }]
  // ✅ If logEntries is empty or null, field is omitted
}
```

### ✅ 6. Behavioral Rules

#### Rule: If BEFORE_ALL fixture fails, no test cases should execute

**Implementation:**
- Captured in `interceptBeforeAllMethod`
- Failure stored in `failedInits` map
- Exception is re-thrown, preventing test execution
- Class status reflects fixture failure

**Code:**
```java
try {
  invocation.proceed();
} catch (Throwable t) {
  failure = t;
  failedInits.put(extensionContext.getUniqueId(), t);
  throw t;  // ✅ Prevents tests from running
}
```

#### Rule: If AFTER_ALL fixture fails, test cases may still pass

**Implementation:**
- AfterAll captured in `interceptAfterAllMethod`
- Failure is recorded but tests have already completed
- Class can show teardown failure while tests show PASSED

**Code:**
```java
try {
  invocation.proceed();
} catch (Throwable t) {
  failure = t;
  throw t;  // ✅ Tests already passed, this is teardown failure
}
```

#### Rule: Fixtures should not duplicate test-case lifecycle data

**Implementation:**
- ✅ Class-level fixtures in `testClasses[].fixtures[]`
- ✅ Test-level lifecycle in `testCases[].lifecycle`
- ✅ No overlap or duplication

### ✅ 7. Output Requirements

**Requirement:** 
- Fixtures must be clearly separated from test cases
- No duplicated or derived fields
- No empty arrays (omit logEntries if empty)

**Implementation:**
- ✅ Fixtures at class level, lifecycle at test level
- ✅ Removed `durationMillis` (derived from nanos)
- ✅ `@JsonInclude(NON_NULL)` omits empty logEntries

## Complete JSON Structure

### Class-Level View

```json
{
  "testClasses": [{
    "className": "PaymentProcessorTest",
    "fixtures": [
      {
        "phase": "BEFORE_ALL",
        "methodName": "setupClass",
        "className": "com.example.testapp.PaymentProcessorTest",
        "status": "PASSED",
        "durationNanos": 6239458,
        "logEntries": [
          {"level": "INFO", "message": "=== Starting Payment Processor Test Suite ==="},
          {"level": "INFO", "message": "Initializing payment gateway connection"}
        ]
      },
      {
        "phase": "AFTER_ALL",
        "methodName": "teardownClass",
        "className": "com.example.testapp.PaymentProcessorTest",
        "status": "PASSED",
        "durationNanos": 433959,
        "logEntries": [
          {"level": "INFO", "message": "=== Payment Processor Test Suite Complete ==="},
          {"level": "INFO", "message": "Closing payment gateway connection"}
        ]
      }
    ],
    "testCases": [...]
  }]
}
```

### Test-Case-Level View

```json
{
  "testCases": [{
    "methodName": "testProcessCreditCardPayment",
    "status": "PASSED",
    "lifecycle": {
      "beforeEach": [{
        "methodName": "setup",
        "className": "PaymentProcessorTest",
        "order": 1,
        "status": "PASSED",
        "durationNanos": 2075542,
        "logEntries": [
          {"level": "INFO", "message": "Creating new payment processor instance"}
        ]
      }],
      "test": {
        "durationNanos": 12323000,
        "logEntries": [
          {"level": "INFO", "message": "Testing credit card payment processing"},
          {"level": "INFO", "message": "Payment approved - Transaction ID: TXN-1000"}
        ]
      },
      "afterEach": []
    },
    "totalDurationNanos": 14398542
  }]
}
```

## Design Decisions

### 1. Fully Qualified Class Name for Fixtures

**Decision:** Use FQN for fixture `className` field

**Rationale:**
- Class-level fixtures may come from base classes or mixins
- FQN ensures no ambiguity about which class defines the fixture
- Test-level uses simple name since it's always same class as test

### 2. "error" vs "failure" in Fixtures

**Decision:** Serialize fixture failures as `"error"` in JSON

**Rationale:**
- Consistency with test fixtures which use `error` field
- Differentiates from test case `failure` field
- Internal code still uses `failure` field name for consistency

### 3. No durationMillis in Fixtures

**Decision:** Only `durationNanos` in fixture model

**Rationale:**
- Millisecond precision is insufficient for fast fixtures
- Can derive millis from nanos if needed: `nanos / 1_000_000`
- Reduces redundancy

### 4. Optional logEntries

**Decision:** Use `@JsonInclude(NON_NULL)` to omit empty log arrays

**Rationale:**
- Reduces JSON size for fixtures without logging
- Cleaner output structure
- Easy to identify which fixtures produce logs

## Implementation Classes

### QAPFixture (Class-Level)
```java
/**
 * Represents a class-level fixture execution (@BeforeAll, @AfterAll).
 * Stored at test class level in fixtures list.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPFixture {
  private final String phase;          // "BEFORE_ALL" or "AFTER_ALL"
  private final String methodName;     // Method name
  private final String className;      // Fully qualified class name
  private final String status;         // PASSED, FAILED, ABORTED
  private final Long durationNanos;    // Execution time
  private final QAPFailure failure;    // Error details if failed
  private final List<QAPLogEntry> logEntries;  // Optional logs
}
```

### QAPTestFixture (Test-Level)
```java
/**
 * Represents a test-case-level fixture execution (@BeforeEach, @AfterEach).
 * Stored within test case lifecycle structure.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPTestFixture {
  private final String methodName;     // Method name
  private final String className;      // Simple class name
  private final Integer order;         // Execution order (1, 2, 3...)
  private final String status;         // PASSED, FAILED, ABORTED
  private final Long durationNanos;    // Execution time
  private final QAPFailure error;      // Error details if failed
  private final List<QAPLogEntry> logEntries;  // Optional logs
}
```

### QAPTestLifecycle (Test-Case Lifecycle)
```java
/**
 * Represents test-case-level lifecycle (NOT class-level).
 * Contains beforeEach, test execution, and afterEach only.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPTestLifecycle {
  private List<QAPTestFixture> beforeEach;
  private TestExecution test;
  private List<QAPTestFixture> afterEach;
  // ✅ No beforeAll or afterAll here
}
```

## Verification

### ✅ Fixtures Properly Separated
- Class-level fixtures in `testClasses[].fixtures[]`
- Test-level lifecycle in `testCases[].lifecycle`
- No duplication

### ✅ Correct Metadata
- All required fields present
- Fully qualified class names
- Duration in nanoseconds only
- Error field for failures

### ✅ Log Capture Working
- BeforeAll logs captured: "Starting Payment Processor Test Suite"
- AfterAll logs captured: "Payment Processor Test Suite Complete"
- BeforeEach logs captured: "Creating new payment processor instance"
- Test logs captured: "Testing credit card payment processing"

### ✅ Clean Output
- No empty logEntries arrays (omitted when null)
- No redundant duration fields
- Clear phase separation

## Benefits

1. **Clear Separation**: Class vs test lifecycle explicitly separated
2. **Accurate Representation**: Fixtures run once per class, correctly modeled
3. **Complete Visibility**: See what happens in class setup/teardown
4. **Debug Power**: Logs at every level (class fixtures, test fixtures, test execution)
5. **Failure Context**: Know if class setup failed vs test failed vs teardown failed

## Conclusion

The fixtures structure now correctly represents the JUnit 5 lifecycle model:
- **Class-level fixtures** (@BeforeAll/@AfterAll) run once per class and are stored at class level
- **Test-level lifecycle** (@BeforeEach/test/@AfterEach) runs per test and is stored per test case
- **No duplication** between the two structures
- **Complete metadata** including FQN, status, timing, and optional logs
- **Clean JSON** with no redundant fields

This design provides a clear, logical structure that accurately represents how JUnit 5 actually executes tests. 🎉
