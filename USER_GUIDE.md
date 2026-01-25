# QAP JUnit 5 Extension - User Guide

A comprehensive JUnit 5 extension for capturing detailed test execution data, lifecycle hooks, logs, and failures in structured JSON format.

---

## Table of Contents

1. [Overview](#overview)
2. [Installation](#installation)
3. [Registering the Extension](#registering-the-extension)
4. [Quick Start](#quick-start)
5. [Using the Extension in Your Tests](#using-the-extension-in-your-tests)
6. [Configuration](#configuration)
7. [Log Capture](#log-capture)
8. [Stack Trace & Failure Information](#stack-trace--failure-information)
9. [Lifecycle Model](#lifecycle-model)
10. [JSON Output Format](#json-output-format)
11. [Troubleshooting](#troubleshooting)
12. [FAQ](#faq)

---

## Overview

### What is QAPJunitExtension?

QAPJunitExtension is a JUnit 5 extension that automatically captures and structures test execution data into JSON reports. It provides comprehensive visibility into test behaviour, including lifecycle hooks, logs, failures, and metadata.

### What Does It Capture?

**Test Execution:**
- Test methods, parameterised tests, nested test classes
- Status (PASSED, FAILED, SKIPPED, ABORTED, DISABLED)
- Execution duration (nanosecond precision)
- Display names and test identifiers

**Lifecycle Hooks:**
- Class-level fixtures (`@BeforeAll`, `@AfterAll`)
- Per-test lifecycle (`@BeforeEach`, test execution, `@AfterEach`)
- Hook execution order, duration, and status
- Logs generated during each phase

**Failures:**
- Exception type and message
- Stack traces (configurable capping)
- User-code location (file, line number)
- Root cause extraction from exception chains
- Suppressed exceptions

**Logs:**
- SLF4J logs captured during test execution
- Phase-isolated (beforeEach logs ≠ test logs)
- Configurable levels (DEBUG, INFO, WARN, ERROR)
- MDC context and markers
- Timestamps with nanosecond precision

**Metadata:**
- Tags (`@Tag` on classes and methods)
- Environment information (OS, JDK, test runner)
- Git information (branch, commit)
- User and environment context

### Why Use It?

- **Debugging**: Understand exactly what happened during test execution
- **CI/CD Integration**: Rich test data for dashboards and reporting services
- **Traceability**: Link test results to specific code versions and environments
- **Analytics**: Historical test data for trend analysis and flakiness detection
- **Compliance**: Detailed audit trail of test execution

---

## Installation

### Gradle

```groovy
dependencies {
    testImplementation 'com.mk.fx.qa:qap-junit-5-plugin:<VERSION>'
    
    // Logging implementation (choose one)
    testRuntimeOnly 'ch.qos.logback:logback-classic:1.4.+'
    // OR
    testRuntimeOnly 'org.apache.logging.log4j:log4j-slf4j2-impl:2.20.+'
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>com.mk.fx.qa</groupId>
        <artifactId>qap-junit-5-plugin</artifactId>
        <version><VERSION></version>
        <scope>test</scope>
    </dependency>
    
    <!-- Logging implementation (choose one) -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.+</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Compatibility

| Component | Minimum Version |
|-----------|----------------|
| JDK | 11+ |
| JUnit Jupiter | 5.8.0+ |
| SLF4J | 1.7.30+ |
| Gradle | 7.0+ (recommended) |
| Maven | 3.6.0+ (recommended) |

**Logging Frameworks:**
- Logback 1.2.0+ (recommended)
- Log4j2 2.17.0+ (supported)
- SLF4J Simple (minimal, not recommended for production)

---

## Registering the Extension

There are two ways to register QAPJunitExtension with JUnit 5:

### Option 1: Explicit Registration (Per Test Class)

Use `@ExtendWith` to register the extension on specific test classes:

```java
import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(QAPJunitExtension.class)
class MyTest {
    @Test
    void testSomething() {
        // test code
    }
}
```

**When to use:**
- ✅ Selective reporting (only some test classes)
- ✅ Testing/debugging the extension
- ✅ Fine-grained control over which tests are captured
- ✅ Multiple modules with different reporting needs

### Option 2: Automatic Registration (All Tests)

Register the extension globally using JUnit 5's ServiceLoader mechanism. The extension will automatically apply to **all test classes** on the classpath.

**Step 1:** Create the service file:

**File:** `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`

**Contents:**
```
com.mk.fx.qa.qap.junit.extension.QAPJunitExtension
```

**Step 2:** Run your tests normally. The extension is automatically applied.

**When to use:**
- ✅ Organisation-wide reporting (capture all tests)
- ✅ CI/CD pipelines (consistent reporting)
- ✅ Single-module projects
- ✅ Simpler test classes (no `@ExtendWith` clutter)

### Automatic Registration: Behaviour & Best Practices

**How it works:**
1. JUnit 5 scans `META-INF/services/` at test runtime
2. Discovers `QAPJunitExtension` automatically
3. Applies it to **every** test class (equivalent to adding `@ExtendWith` everywhere)

**Extension ordering:**
- If you register multiple extensions via `META-INF/services/`, the order is **not guaranteed**
- To control order, use explicit `@ExtendWith` annotations with `@Order`

**Disabling in specific modules:**

If you use automatic registration but want to disable it in a specific module:

```groovy
// Gradle: Exclude the service file
test {
    classpath = classpath.filter { 
        !it.path.contains('META-INF/services/org.junit.jupiter.api.extension.Extension') 
    }
}
```

Or override with a module-specific configuration.

**Warnings:**
- ⚠️ Automatic registration captures **all tests** - be mindful of performance in large test suites
- ⚠️ Cannot selectively disable for individual test classes (use explicit registration if needed)
- ⚠️ May conflict with other extensions that modify test execution order

**Recommendation:**
- Use **automatic registration** for production/CI reporting (consistent, no annotation overhead)
- Use **explicit registration** for development/debugging or selective reporting

---

## Quick Start

### Minimal Test Example (Explicit Registration)

```java
package com.example;

import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(QAPJunitExtension.class)
@DisplayName("Calculator Tests")
@Tag("unit")
class CalculatorTest {

    @Test
    @DisplayName("Should add two numbers correctly")
    @Tag("arithmetic")
    void testAddition() {
        int result = 2 + 2;
        assertEquals(4, result);
    }
}
```

**With automatic registration (META-INF):** The same test works without `@ExtendWith` - simply remove that line.

### What Gets Produced

After running the test, QAPJunitExtension generates a JSON report to the console (configurable):

```json
{
  "header": {
    "launchId": "TestLaunch-abc123",
    "applicationName": "MyApp",
    "testEnvironment": "TEST"
  },
  "testClasses": [{
    "className": "CalculatorTest",
    "displayName": "Calculator Tests",
    "testCases": [{
      "methodName": "testAddition",
      "displayName": "Should add two numbers correctly",
      "status": "PASSED",
      "totalDurationNanos": 1250000,
      "tags": {
        "class": ["unit"],
        "method": ["arithmetic"]
      }
    }]
  }]
}
```

---

## Using the Extension in Your Tests

### Supported JUnit 5 Annotations

QAPJunitExtension automatically captures behaviour from these annotations:

**Test Methods:**
- `@Test` - Standard test methods
- `@ParameterizedTest` - Parameterised test invocations (each invocation is a separate test case)
- `@RepeatedTest` - Repeated test executions

**Lifecycle Hooks:**
- `@BeforeAll` - Class-level setup (captured as fixtures)
- `@AfterAll` - Class-level teardown (captured as fixtures)
- `@BeforeEach` - Per-test setup (captured in lifecycle)
- `@AfterEach` - Per-test teardown (captured in lifecycle)

**Organisation:**
- `@Nested` - Nested test classes (captured with full hierarchy)
- `@DisplayName` - Human-readable names (used in JSON output)
- `@Tag` - Test categorisation (captured in `tags` field)
- `@Disabled` - Skipped tests (status: DISABLED, reason captured)

### How Tags Are Handled

**Class-level tags:**
```java
@Tag("integration")
@Tag("database")
class UserRepositoryTest {
    // All tests inherit these tags
}
```

**Method-level tags:**
```java
@Test
@Tag("smoke")
void testCriticalPath() {
    // Has: integration, database, smoke
}
```

**JSON output:**
```json
{
  "tags": {
    "class": ["integration", "database"],
    "method": ["smoke"],
    "inherited": ["integration", "database"]
  }
}
```

### Parameterised Tests

Each parameterised test invocation gets a unique `testCaseId`:

```java
@ParameterizedTest
@ValueSource(strings = {"apple", "banana", "cherry"})
void testFruits(String fruit) {
    assertNotNull(fruit);
}
```

**JSON output:**
```json
{
  "testCases": [
    {
      "testCaseId": "FruitTest#testFruits[0]",
      "displayName": "testFruits(String) - [0] apple",
      "parameters": [{"index": 0, "type": "String", "value": "apple"}],
      "parameterization": {"provider": "ValueSource", "invocationIndex": 0}
    },
    {
      "testCaseId": "FruitTest#testFruits[1]",
      "displayName": "testFruits(String) - [1] banana",
      "parameters": [{"index": 1, "type": "String", "value": "banana"}],
      "parameterization": {"provider": "ValueSource", "invocationIndex": 1}
    }
  ]
}
```

### Nested Test Classes

```java
@DisplayName("Order Processing")
class OrderTest {
    
    @Nested
    @DisplayName("Payment Operations")
    class PaymentTests {
        
        @Test
        void testSuccessfulPayment() { }
    }
}
```

**JSON hierarchy:**
```json
{
  "testClasses": [{
    "displayName": "Order Processing",
    "children": [{
      "displayName": "Payment Operations",
      "parentChain": ["Order Processing", "Payment Operations"]
    }]
  }]
}
```

### Best Practices

**Use stable display names:**
```java
// Good
@DisplayName("Should process payment successfully")

// Avoid
@DisplayName("Test #1234 - 2024-01-15")  // Timestamps make diffs noisy
```

**Avoid flaky ordering assumptions:**
```java
// Good
@BeforeEach
void setUp() {
    // Isolated setup per test
}

// Avoid
static int counter = 0;  // Shared state between tests
```

**Use deterministic test data:**
```java
// Good
@ParameterizedTest
@ValueSource(ints = {1, 2, 3})

// Avoid
@ParameterizedTest
@ValueSource(ints = {(int) System.currentTimeMillis()})  // Non-deterministic
```

**Prefer structured logging:**
```java
// Good
logger.info("Processing payment for user: {}", userId);

// Avoid
logger.info("Processing payment for user: " + userId);  // Harder to parse
```

---

## Configuration

### How Configuration Works

QAPJunitExtension loads configuration from multiple sources (in order of precedence):

1. **System properties** (highest priority): `-Dqap.app.name=MyApp`
2. **qap.properties file**: `src/test/resources/qap.properties`
3. **Built-in defaults** (lowest priority)

### Creating qap.properties

**File location:** `src/test/resources/qap.properties`

**Example configuration:**

```properties
# ========================================
# Application & Environment
# ========================================
qap.app.name=MyApplication
qap.test.environment=TEST
qap.run.environment=CI
qap.user=jenkins

# Regression flag
qap.regression=false

# ========================================
# Reporting
# ========================================
# Enable/disable JSON report generation
qap.reporting.enabled=true

# API integration (optional)
qap.api.key=<YOUR_API_KEY>

# ========================================
# Log Capture
# ========================================
# Enable/disable log capture
qap.logging.enabled=true

# Minimum log level to capture
qap.logging.min.level=DEBUG

# Limits (prevent memory issues)
qap.logging.max.entries=1000
qap.logging.max.message.length=10000

# What to include
qap.logging.capture.stacktraces=true
qap.logging.include.mdc=true
qap.logging.include.markers=true

# Filter by logger name (optional - comma-separated patterns)
qap.logging.logger.patterns=com.example.*,org.myapp.*

# ========================================
# Stack Trace Configuration
# ========================================
# Maximum total lines (-1 = unlimited)
qap.stacktrace.max.lines=200

# Head + Tail strategy (default)
qap.stacktrace.head.lines=50
qap.stacktrace.tail.lines=20

# Alternative: User-code only strategy
qap.stacktrace.keep.until.framework.exit=false
```

### Configuration Reference

#### Application & Environment

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `qap.app.name` | String | `null` | Application name (e.g., "PaymentService") |
| `qap.test.environment` | String | `null` | Test environment (e.g., "TEST", "UAT", "PROD") |
| `qap.run.environment` | String | `UAT` | Execution environment (e.g., "CI", "LOCAL", "DEV") |
| `qap.user` | String | System user | Test executor username |
| `qap.regression` | Boolean | `false` | Flag to mark test run as regression suite |

#### Reporting

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `qap.reporting.enabled` | Boolean | `true` | Enable JSON report generation |
| `qap.api.key` | String | `null` | API key for uploading reports to QAP service |

#### Log Capture

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `qap.logging.enabled` | Boolean | `true` | Enable log capture during test execution |
| `qap.logging.min.level` | String | `DEBUG` | Minimum log level: TRACE, DEBUG, INFO, WARN, ERROR, FATAL |
| `qap.logging.max.entries` | Integer | `1000` | Maximum log entries per test (prevents OOM) |
| `qap.logging.max.message.length` | Integer | `10000` | Truncate messages longer than this |
| `qap.logging.capture.stacktraces` | Boolean | `true` | Include exception stack traces in logs |
| `qap.logging.include.mdc` | Boolean | `true` | Include MDC (Mapped Diagnostic Context) |
| `qap.logging.include.markers` | Boolean | `true` | Include SLF4J markers |
| `qap.logging.logger.patterns` | String | Empty | Comma-separated logger patterns (e.g., "com.example.*") |

#### Stack Trace Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `qap.stacktrace.max.lines` | Integer | `200` | Maximum stack trace lines (-1 = unlimited) |
| `qap.stacktrace.head.lines` | Integer | `50` | Keep first N lines (when using head+tail strategy) |
| `qap.stacktrace.tail.lines` | Integer | `20` | Keep last N lines (when using head+tail strategy) |
| `qap.stacktrace.keep.until.framework.exit` | Boolean | `false` | Stop at framework boundary (smaller traces) |

### Stack Trace Strategies

**Strategy 1: Head + Tail (Default)**
```properties
qap.stacktrace.keep.until.framework.exit=false
qap.stacktrace.head.lines=50
qap.stacktrace.tail.lines=20
```
**Result:** First 50 + last 20 lines, omitted section in between  
**Use case:** Balanced - see error location AND test entry point

**Strategy 2: User-Code Only**
```properties
qap.stacktrace.keep.until.framework.exit=true
qap.stacktrace.max.lines=100
```
**Result:** Stops when exiting user code (ignores framework tail)  
**Use case:** Minimal payloads - focus on your code execution path

### CI/CD Configuration

**Gradle:**
```groovy
test {
    // Override properties for CI
    systemProperty 'qap.test.environment', 'CI'
    systemProperty 'qap.user', System.getenv('BUILD_USER') ?: 'jenkins'
    
    // Enable parallel execution (extension is thread-safe)
    maxParallelForks = Runtime.runtime.availableProcessors()
}
```

**Maven:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <qap.test.environment>CI</qap.test.environment>
            <qap.user>${env.BUILD_USER}</qap.user>
        </systemPropertyVariables>
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

---

## Log Capture

### What Is Captured

QAPJunitExtension captures SLF4J logs generated during test execution. Logs are **phase-isolated**, meaning each lifecycle phase has its own log collection:

**Class-level fixtures:**
- `@BeforeAll` logs → stored in `fixtures[].logEntries`
- `@AfterAll` logs → stored in `fixtures[].logEntries`

**Per-test lifecycle:**
- `@BeforeEach` logs → stored in `lifecycle.beforeEach[].logEntries`
- Test method logs → stored in `lifecycle.test.logEntries`
- `@AfterEach` logs → stored in `lifecycle.afterEach[].logEntries`

**Isolation guarantees:**
- Logs from `@BeforeEach` do NOT appear in test method logs
- Logs from test method do NOT appear in `@AfterEach` logs
- Each phase sees only its own logs

### Log Entry Structure

Each captured log entry contains:

```json
{
  "timestamp": 1737734567.123456789,
  "level": "INFO",
  "logger": "com.example.MyTest",
  "thread": "Test worker",
  "message": "Processing payment for user: john.doe",
  "mdc": {
    "userId": "12345",
    "requestId": "abc-def-ghi"
  },
  "markers": ["AUDIT"]
}
```

**Fields:**
- `timestamp` - Unix epoch seconds with nanosecond precision
- `level` - Log level (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
- `logger` - Logger name (typically class name)
- `thread` - Thread name executing the test
- `message` - Formatted log message
- `mdc` - Mapped Diagnostic Context (key-value pairs)
- `markers` - SLF4J markers (for structured logging)

### Log Level Rules

**Default behaviour:**
- Captures DEBUG and above (DEBUG, INFO, WARN, ERROR, FATAL)
- TRACE is excluded by default (too verbose)

**Enable TRACE logs:**
```properties
qap.logging.min.level=TRACE
```

**Capture only warnings and errors:**
```properties
qap.logging.min.level=WARN
```

**WARN/ERROR priority:**
- WARN and ERROR logs are **always** included (regardless of `max.entries`)
- INFO/DEBUG logs may be capped to stay within `max.entries` limit
- This ensures critical logs are never lost

### Performance & Payload Size

**Max entries limit:**
```properties
qap.logging.max.entries=1000
```
- Prevents memory issues with verbose tests
- When limit is reached, oldest INFO/DEBUG logs are dropped
- WARN/ERROR logs are preserved

**Message truncation:**
```properties
qap.logging.max.message.length=10000
```
- Long messages are truncated to this length
- Prevents massive payloads from stack traces or serialised objects

**Strategy for high-volume tests:**
```properties
# Minimal configuration
qap.logging.enabled=true
qap.logging.min.level=ERROR
qap.logging.max.entries=100
qap.logging.include.mdc=false
qap.logging.include.markers=false
```

### Filtering Logs

**Capture only application logs:**
```properties
qap.logging.logger.patterns=com.example.*,com.myapp.*
```

**Exclude framework noise:**
```properties
# Only capture your code
qap.logging.logger.patterns=com.example.*
# This excludes: org.springframework.*, org.hibernate.*, etc.
```

### Avoiding Sensitive Data

**Mask sensitive values:**
```java
// Good
logger.info("Payment processed for card: ****{}", card.lastFour());

// Avoid
logger.info("Payment processed for card: {}", card.fullNumber());  // PCI violation
```

**Use MDC for structured data:**
```java
// Good
MDC.put("userId", userId);
logger.info("Payment processed");
MDC.clear();

// Avoid
logger.info("Payment processed for userId=" + userId);  // Harder to mask
```

**Disable MDC if it contains secrets:**
```properties
qap.logging.include.mdc=false
```

---

## Stack Trace & Failure Information

### When Is `failure` Populated?

The `failure` field is populated when a test fails or throws an exception:

**Scenarios:**
- ✅ Assertion failures (`assertEquals`, `assertTrue`, etc.)
- ✅ Runtime exceptions (NPE, IllegalArgumentException, etc.)
- ✅ Timeouts (`@Timeout` exceeded)
- ✅ BeforeEach/AfterEach failures (captured in lifecycle hooks)

**Not populated for:**
- ❌ Passing tests (status: PASSED)
- ❌ Skipped tests (status: SKIPPED, use `disabledReason` instead)
- ❌ Disabled tests (status: DISABLED)

### Failure Structure

```json
{
  "failure": {
    "type": "org.opentest4j.AssertionFailedError",
    "message": "expected: <5> but was: <3>",
    "stackTrace": [
      "org.opentest4j.AssertionFailedError: expected: <5> but was: <3>",
      "at org.junit.jupiter.api.Assertions.assertEquals(...)",
      "at com.example.CalculatorTest.testAddition(CalculatorTest.java:42)",
      "... 47 more lines omitted ..."
    ],
    "location": {
      "class": "com.example.CalculatorTest",
      "method": "testAddition",
      "file": "CalculatorTest.java",
      "line": 42
    },
    "rootCause": null,
    "suppressed": []
  }
}
```

### Failure Fields

| Field | Type | Description |
|-------|------|-------------|
| `type` | String | Fully qualified exception class name |
| `message` | String | Exception message |
| `stackTrace` | String[] | Stack trace lines (capped according to config) |
| `location` | Object | First user-code frame (where YOUR code failed) |
| `rootCause` | Object | Root cause for chained exceptions (null if none) |
| `suppressed` | Array | Suppressed exceptions (Java try-with-resources) |
| `causedBy` | Object | Direct cause (for exception chains) |

### How `failure.location` Is Derived

**Problem:** JUnit assertion failures point to JUnit internals:
```
at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:183)  ← Framework
at com.example.MyTest.testSomething(MyTest.java:42)                    ← YOUR CODE
```

**Solution:** QAPJunitExtension extracts the **first user-code frame**:
```json
{
  "location": {
    "class": "com.example.MyTest",
    "method": "testSomething",
    "file": "MyTest.java",
    "line": 42
  }
}
```

**Frame detection:**
- Skips: `org.junit.*`, `org.opentest4j.*`, `java.base/*`, `org.gradle.*`, JDK internals
- Finds: First frame matching your package structure
- Fallback: If all frames are framework (unlikely), uses first frame

**Result:** `failure.location` always points to **your test code**, not JUnit internals.

### Assertion Failures vs Runtime Exceptions

**Assertion failure:**
```json
{
  "failure": {
    "type": "org.opentest4j.AssertionFailedError",
    "message": "expected: <true> but was: <false>",
    "location": {"class": "MyTest", "line": 42}
  }
}
```

**Runtime exception:**
```json
{
  "failure": {
    "type": "java.lang.NullPointerException",
    "message": "Cannot invoke method on null object",
    "location": {"class": "MyService", "line": 123}
  }
}
```

**Chained exception (with root cause):**
```json
{
  "failure": {
    "type": "java.lang.RuntimeException",
    "message": "Failed to process payment",
    "rootCause": {
      "type": "java.sql.SQLException",
      "message": "Connection timeout"
    },
    "causedBy": {
      "type": "java.sql.SQLException",
      "message": "Connection timeout"
    }
  }
}
```

### Using Logs + Stack Trace Together

**Example scenario:**
1. Test fails with NPE at line 42
2. Look at `lifecycle.test.logEntries` to see what happened before the failure
3. Check `failure.stackTrace` to see the execution path
4. Use `failure.location` to jump directly to the failing line

**Debugging workflow:**
```
1. Check testCase.status = FAILED
2. Read failure.message for quick context
3. Review lifecycle.test.logEntries for runtime behaviour
4. Examine failure.stackTrace for execution path
5. Jump to failure.location.line to fix the issue
```

---

## Lifecycle Model

### Fixtures vs Lifecycle

QAPJunitExtension distinguishes between **class-level** and **test-level** hooks:

**Fixtures (Class-Level):**
- Run once per test class
- Captured in `testClasses[].fixtures[]`
- Includes: `@BeforeAll`, `@AfterAll`
- Not duplicated per test

**Lifecycle (Per-Test):**
- Run for each test method
- Captured in `testCases[].lifecycle`
- Includes: `@BeforeEach`, test execution, `@AfterEach`
- Unique per test case

### Why Fixtures Don't Appear at Test-Case Level

**Common question:** "Why isn't `@BeforeAll` in my test's lifecycle?"

**Answer:** Because `@BeforeAll` runs **once per class**, not per test. Duplicating it for every test case would be:
- ❌ Misleading (implies it ran multiple times)
- ❌ Wasteful (bloats JSON payload)
- ❌ Incorrect (misrepresents test execution)

**Example:**
```java
class MyTest {
    @BeforeAll
    static void setupClass() { }  // Runs ONCE
    
    @BeforeEach
    void setUp() { }              // Runs for EACH test
    
    @Test void test1() { }
    @Test void test2() { }
    @Test void test3() { }
}
```

**JSON structure:**
```json
{
  "testClasses": [{
    "fixtures": [
      {"phase": "BEFORE_ALL", "methodName": "setupClass"}  // Once at class level
    ],
    "testCases": [
      {
        "methodName": "test1",
        "lifecycle": {
          "beforeEach": [{"methodName": "setUp"}],  // Per test
          "test": { },
          "afterEach": []
        }
      },
      {
        "methodName": "test2",
        "lifecycle": {
          "beforeEach": [{"methodName": "setUp"}],  // Per test
          "test": { },
          "afterEach": []
        }
      }
    ]
  }]
}
```

### Example JSON Fragments

**Passing test:**
```json
{
  "methodName": "testSuccessfulPayment",
  "status": "PASSED",
  "lifecycle": {
    "beforeEach": [
      {
        "methodName": "setUp",
        "status": "PASSED",
        "durationNanos": 1250000,
        "logEntries": [
          {"level": "INFO", "message": "Initialising test data"}
        ]
      }
    ],
    "test": {
      "durationNanos": 5430000,
      "logEntries": [
        {"level": "INFO", "message": "Processing payment"},
        {"level": "INFO", "message": "Payment approved"}
      ]
    },
    "afterEach": []
  }
}
```

**Failing assertion:**
```json
{
  "methodName": "testInvalidAmount",
  "status": "FAILED",
  "lifecycle": {
    "beforeEach": [
      {
        "methodName": "setUp",
        "status": "PASSED",
        "durationNanos": 980000
      }
    ],
    "test": {
      "durationNanos": 2340000,
      "logEntries": [
        {"level": "WARN", "message": "Invalid amount: -10"},
        {"level": "ERROR", "message": "Validation failed"}
      ]
    },
    "afterEach": []
  },
  "failure": {
    "type": "org.opentest4j.AssertionFailedError",
    "message": "expected: <true> but was: <false>",
    "location": {
      "class": "com.example.PaymentTest",
      "method": "testInvalidAmount",
      "line": 67
    }
  }
}
```

**BeforeEach failure:**
```json
{
  "methodName": "testPayment",
  "status": "FAILED",
  "lifecycle": {
    "beforeEach": [
      {
        "methodName": "setUp",
        "status": "FAILED",
        "durationNanos": 450000,
        "error": {
          "type": "java.lang.NullPointerException",
          "message": "Cannot initialise null database"
        }
      }
    ],
    "test": {
      "durationNanos": 0
    },
    "afterEach": []
  },
  "failure": {
    "type": "java.lang.NullPointerException",
    "message": "Cannot initialise null database"
  }
}
```
**Note:** When `@BeforeEach` fails, the test method doesn't execute (duration = 0).

### Status Resolution Rules

**Canonical status:** `testCase.status` is the authoritative status field.

**Status priority (from highest to lowest):**
1. **FAILED** - Any failure in beforeEach, test, or afterEach
2. **ABORTED** - Test was aborted (e.g., assumption failed)
3. **DISABLED** - Test was skipped via `@Disabled`
4. **SKIPPED** - Test was skipped for other reasons
5. **PASSED** - All phases succeeded

**Examples:**
- BeforeEach passes, test passes, afterEach passes → **PASSED**
- BeforeEach passes, test fails, afterEach passes → **FAILED**
- BeforeEach fails, test doesn't run, afterEach doesn't run → **FAILED**
- BeforeEach passes, test passes, afterEach fails → **FAILED** (afterEach errors still fail the test)

---

## JSON Output Format

### Top-Level Structure

```json
{
  "header": {
    "launchStartTime": 1737734567123,
    "launchEndTime": 1737734598456,
    "launchId": "TestLaunch-abc123def456",
    "applicationName": "MyApp",
    "testEnvironment": "TEST",
    "user": "jenkins",
    "gitBranch": "main",
    "osVersion": "Linux 5.15",
    "testRunnerVersion": "Junit 5.10.2",
    "jdkVersion": "JDK 17.0.5",
    "regression": false
  },
  "testClasses": [
    {
      "className": "PaymentProcessorTest",
      "displayName": "Payment Processor Tests",
      "classFqn": "com.example.PaymentProcessorTest",
      "classSimpleName": "PaymentProcessorTest",
      "fixtures": [],
      "testCases": [],
      "children": [],
      "tags": {"class": ["integration", "payment"]}
    }
  ]
}
```

### Complete Example (One Class, One Test)

```json
{
  "header": {
    "launchStartTime": 1737734567123,
    "launchId": "TestLaunch-abc123",
    "applicationName": "PaymentService",
    "testEnvironment": "TEST",
    "user": "jenkins",
    "osVersion": "Mac OS X 26.2",
    "testRunnerVersion": "Junit 5.10.2",
    "jdkVersion": "JDK 17.0.5",
    "regression": false,
    "launchEndTime": 1737734598456
  },
  "testClasses": [
    {
      "className": "PaymentProcessorTest",
      "displayName": "Payment Processor Tests",
      "classFqn": "com.example.testapp.PaymentProcessorTest",
      "classSimpleName": "PaymentProcessorTest",
      "fixtures": [
        {
          "phase": "BEFORE_ALL",
          "methodName": "setupClass",
          "className": "com.example.testapp.PaymentProcessorTest",
          "status": "PASSED",
          "durationNanos": 5025000,
          "logEntries": [
            {
              "timestamp": 1737734567.262000000,
              "level": "INFO",
              "logger": "com.example.testapp.PaymentProcessorTest",
              "thread": "Test worker",
              "message": "Initialising payment gateway",
              "mdc": {},
              "markers": []
            }
          ]
        },
        {
          "phase": "AFTER_ALL",
          "methodName": "teardownClass",
          "className": "com.example.testapp.PaymentProcessorTest",
          "status": "PASSED",
          "durationNanos": 350417,
          "logEntries": [
            {
              "timestamp": 1737734598.334000000,
              "level": "INFO",
              "logger": "com.example.testapp.PaymentProcessorTest",
              "thread": "Test worker",
              "message": "Closing payment gateway",
              "mdc": {},
              "markers": []
            }
          ]
        }
      ],
      "testCases": [
        {
          "startTime": 1737734567329,
          "endTime": 1737734567330,
          "status": "FAILED",
          "methodName": "testMinimumPaymentAmount",
          "displayName": "Should reject payment below minimum amount",
          "testCaseId": "PaymentProcessorTest#testMinimumPaymentAmount",
          "methodDisplayName": "Should reject payment below minimum amount",
          "parameters": [],
          "parameterization": null,
          "testType": "TEST",
          "disabledReason": null,
          "lifecycle": {
            "beforeEach": [
              {
                "methodName": "setup",
                "className": "com.example.testapp.PaymentProcessorTest",
                "order": 1,
                "status": "PASSED",
                "durationNanos": 1957000,
                "logEntries": [
                  {
                    "timestamp": 1737734567.329000000,
                    "level": "INFO",
                    "logger": "com.example.testapp.PaymentProcessorTest",
                    "thread": "Test worker",
                    "message": "Creating payment processor instance",
                    "mdc": {},
                    "markers": []
                  }
                ]
              }
            ],
            "test": {
              "durationNanos": 13846792,
              "logEntries": [
                {
                  "timestamp": 1737734567.329000000,
                  "level": "INFO",
                  "logger": "com.example.testapp.PaymentProcessorTest",
                  "thread": "Test worker",
                  "message": "Testing minimum payment validation",
                  "mdc": {},
                  "markers": []
                },
                {
                  "timestamp": 1737734567.330000000,
                  "level": "WARN",
                  "logger": "com.example.testapp.PaymentProcessorTest",
                  "thread": "Test worker",
                  "message": "Payment below minimum: $0.50",
                  "mdc": {},
                  "markers": []
                },
                {
                  "timestamp": 1737734567.330000000,
                  "level": "ERROR",
                  "logger": "com.example.testapp.PaymentProcessorTest",
                  "thread": "Test worker",
                  "message": "Payment rejected: Amount below minimum",
                  "mdc": {},
                  "markers": []
                }
              ]
            },
            "afterEach": []
          },
          "failure": {
            "type": "org.opentest4j.AssertionFailedError",
            "message": "expected: <true> but was: <false>",
            "stackTrace": [
              "org.opentest4j.AssertionFailedError: expected: <true> but was: <false>",
              "at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)",
              "at com.example.testapp.PaymentProcessorTest.testMinimumPaymentAmount(PaymentProcessorTest.java:116)",
              "\t... 90 more lines omitted ..."
            ],
            "suppressed": [],
            "location": {
              "class": "com.example.testapp.PaymentProcessorTest",
              "method": "testMinimumPaymentAmount",
              "file": "PaymentProcessorTest.java",
              "line": 116
            }
          },
          "totalDurationNanos": 15803792,
          "tags": {
            "class": ["financial", "payment"]
          }
        }
      ],
      "children": [],
      "parentChain": ["Payment Processor Tests"],
      "tags": {
        "class": ["financial", "payment"]
      }
    }
  ]
}
```

### Optional / Omitted Fields

**Fields omitted when empty/null:**
- `logEntries` - Only present when logs exist
- `failure` - Only present when test fails
- `disabledReason` - Only present when test is disabled
- `parameterization` - Only present for parameterised tests
- `children` - Only present for nested test classes
- `rootCause` - Only present when exception has a cause chain
- `suppressed` - Only present when exceptions are suppressed

**Empty arrays are omitted:**
- `lifecycle.afterEach: []` - Omitted if no afterEach hooks
- `fixtures: []` - Omitted if no class-level hooks

**Result:** Smaller JSON payloads, easier to read.

---

## Troubleshooting

### Extension Not Running

**Symptom:** No JSON output, tests run normally

**Causes & solutions:**

1. **META-INF/services file missing or misconfigured**
   ```
   # Check file exists
   src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension
   
   # Verify contents (exact line, no trailing spaces)
   com.mk.fx.qa.qap.junit.extension.QAPJunitExtension
   ```

2. **Missing @ExtendWith annotation** (if not using META-INF)
   ```java
   @ExtendWith(QAPJunitExtension.class)  // Add this
   class MyTest { }
   ```

3. **Extension JAR not on classpath**
   ```groovy
   // Gradle: Check dependency
   dependencies {
       testImplementation 'com.mk.fx.qa:qap-junit-5-plugin:<VERSION>'
   }
   ```

4. **JUnit version incompatible**
   ```groovy
   // Require JUnit 5.8.0+
   testImplementation 'org.junit.jupiter:junit-jupiter:5.10.+'
   ```

### Missing DEBUG Logs

**Symptom:** Only see INFO/WARN/ERROR, no DEBUG logs

**Solution:**
```properties
# qap.properties
qap.logging.min.level=DEBUG
```

**Verify logging configuration:**
```xml
<!-- logback-test.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="DEBUG">  <!-- Must be DEBUG or lower -->
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### Duplicate Logs Across Phases

**Symptom:** Same log appears in beforeEach AND test

**Cause:** Misconfigured log capture window

**Solution:** This should not happen with QAPJunitExtension's phase-isolated capture. If you see this:
1. Check for custom logging setup interfering
2. Verify you're using the latest version
3. Check for duplicate extension registration

### No JSON Output

**Symptom:** Tests run, extension loads, but no JSON produced

**Causes & solutions:**

1. **Reporting disabled**
   ```properties
   qap.reporting.enabled=true  # Must be true
   ```

2. **Check console output** - JSON is printed to STDOUT by default

3. **Check for exceptions** - Look for QAP-related errors in logs

### CI vs Local Differences

**Symptom:** Works locally, fails in CI

**Common causes:**

1. **Environment variables not set**
   ```bash
   # CI pipeline
   export QAP_TEST_ENVIRONMENT=CI
   export QAP_USER=jenkins
   ```

2. **Different JDK versions**
   ```properties
   # CI: Specify exact version
   qap.jdk.version=17.0.5
   ```

3. **Parallel execution issues**
   ```groovy
   // Gradle: Ensure extension is thread-safe (it is)
   test {
       maxParallelForks = 4  // Safe with QAPJunitExtension
   }
   ```

4. **Missing qap.properties in CI**
   ```bash
   # CI: Copy from secure location
   cp /var/secrets/qap.properties src/test/resources/
   ```

### Validate JSON Output

**Quick validation:**
```bash
# Pretty-print and check for errors
./gradlew test | grep '{"header"' | jq . > test-report.json
```

**Schema validation (example):**
```bash
# Using ajv-cli
npm install -g ajv-cli
ajv validate -s qap-schema.json -d test-report.json
```

---

## FAQ

### Should fixtures appear on each test case?

**No.** Fixtures (`@BeforeAll`, `@AfterAll`) run **once per class**, not per test. They appear at `testClasses[].fixtures[]`, not in individual test cases.

**Rationale:**
- Fixtures run once per class (that's what JUnit 5 guarantees)
- Duplicating them per test would misrepresent execution
- Bloats JSON payload unnecessarily

**If you need per-test setup:** Use `@BeforeEach` and `@AfterEach` - these appear in `testCases[].lifecycle`.

---

### Do I need @ExtendWith if META-INF is used?

**No.** If you've registered the extension via `META-INF/services/`, it applies automatically to all tests.

**Choose one approach:**
- **META-INF**: Automatic, organisation-wide reporting
- **@ExtendWith**: Explicit, selective reporting

**Don't use both** - it's redundant and may cause duplicate registration.

---

### Why do I see WARN/ERROR but not DEBUG?

**Reason:** Default minimum log level is `DEBUG`, but your logger might be set to INFO.

**Solution 1:** Enable DEBUG in QAP:
```properties
qap.logging.min.level=DEBUG
```

**Solution 2:** Enable DEBUG in your logging framework:
```xml
<!-- logback-test.xml -->
<root level="DEBUG">
    <appender-ref ref="CONSOLE"/>
</root>
```

**Note:** Both must be DEBUG for QAP to capture DEBUG logs.

---

### How are parameterised tests represented?

Each parameterised test invocation gets:
- Unique `testCaseId`: `MyTest#myTest[0]`, `MyTest#myTest[1]`, etc.
- `parameterization` object with provider and invocation index
- `parameters` array with actual values
- Unique `displayName` with parameter values

**Example:**
```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3})
void testNumbers(int num) { }
```

**Result:**
```json
{
  "testCases": [
    {
      "testCaseId": "MyTest#testNumbers[0]",
      "displayName": "testNumbers(int) - [0] 1",
      "parameters": [{"index": 0, "type": "int", "value": "1"}],
      "parameterization": {"provider": "ValueSource", "invocationIndex": 0}
    }
  ]
}
```

---

### How do I reduce payload size?

**1. Reduce log capture:**
```properties
qap.logging.min.level=WARN         # Only warnings and errors
qap.logging.max.entries=100        # Limit entries per test
qap.logging.include.mdc=false      # Skip MDC
qap.logging.include.markers=false  # Skip markers
```

**2. Cap stack traces:**
```properties
qap.stacktrace.keep.until.framework.exit=true  # User-code only
qap.stacktrace.max.lines=50                    # Smaller limit
```

**3. Filter loggers:**
```properties
qap.logging.logger.patterns=com.example.*  # Only your code
```

**4. Disable for passing tests (future enhancement):**
```properties
# Not currently supported - capture all tests
```

**Result:** 50-75% smaller JSON payloads.

---

### Can I use this with JUnit 4?

**No.** QAPJunitExtension is designed specifically for **JUnit 5** (JUnit Jupiter).

**Migration path:**
1. Migrate tests from JUnit 4 to JUnit 5
2. Use JUnit 5's vintage engine for gradual migration
3. Apply QAPJunitExtension to JUnit 5 tests only

---

### Is the extension thread-safe?

**Yes.** QAPJunitExtension is fully thread-safe and supports parallel test execution.

**Safe configurations:**
```groovy
// Gradle
test {
    maxParallelForks = Runtime.runtime.availableProcessors()
}
```

```xml
<!-- Maven -->
<configuration>
    <parallel>methods</parallel>
    <threadCount>4</threadCount>
</configuration>
```

---

### How do I send reports to a QAP service?

**Step 1:** Configure API key:
```properties
qap.api.key=<YOUR_API_KEY>
```

**Step 2:** The extension automatically uploads JSON to the configured endpoint (implementation-specific).

**Manual upload:**
```bash
# Extract JSON from test output
./gradlew test | grep '{"header"' > report.json

# Upload via curl
curl -X POST https://qap-service.example.com/reports \
  -H "Authorization: Bearer <API_KEY>" \
  -H "Content-Type: application/json" \
  -d @report.json
```

---

### Can I customise the JSON output location?

**Currently:** JSON is printed to STDOUT (console).

**Capture to file:**
```bash
# Gradle
./gradlew test | grep '{"header"' > test-reports/qap-report.json

# Maven
mvn test | grep '{"header"' > test-reports/qap-report.json
```

**Future enhancement:** Configurable output directory (not yet implemented).

---

## Summary

QAPJunitExtension provides rich, structured test reporting for JUnit 5, capturing:

✅ Test execution data (status, duration, parameters)  
✅ Lifecycle hooks with phase-isolated logs  
✅ Failures with user-code locations and stack traces  
✅ Class-level fixtures (once per class)  
✅ Per-test lifecycle (once per test)  
✅ Configurable log capture (levels, limits, filtering)  
✅ Minimal JSON payloads (omit empty/null fields)

**Get started:**
1. Add dependency to `build.gradle` or `pom.xml`
2. Register via `@ExtendWith` or `META-INF/services`
3. Create `qap.properties` for configuration
4. Run tests and collect JSON reports

**Need help?** Check [Troubleshooting](#troubleshooting) and [FAQ](#faq).

---

**Version:** 1.0  
**Last Updated:** January 2026  
**License:** <YOUR_LICENSE>
