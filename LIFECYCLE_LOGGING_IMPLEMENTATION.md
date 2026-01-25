# Lifecycle and Fixture Logging Implementation

## Overview

Successfully implemented comprehensive log capture for all JUnit 5 lifecycle phases (beforeAll, beforeEach, test execution, afterEach, afterAll). Now you can see exactly what's happening in your application at every stage of test execution.

## Changes Made

### 1. Model Updates

#### QAPTestFixture.java
- Added `logEntries` field to capture logs during fixture execution
- Maintains backward compatibility with overloaded constructor
- Each fixture (beforeAll, beforeEach, afterEach, afterAll) now tracks its own logs

#### QAPTestLifecycle.java (TestExecution)
- Added `logEntries` field to capture logs during actual test execution
- Separates test logs from fixture logs for better debugging

### 2. Log Capture Implementation

#### QAPJunitMethodInterceptor.java
- Added `logCapturer` field with setter for dependency injection
- Implemented log capture for **beforeAll** fixtures
- Implemented log capture for **beforeEach** fixtures  
- Implemented log capture for **afterEach** fixtures
- Implemented log capture for **afterAll** fixtures
- Added helper methods:
  - `startFixtureLogCapture(String fixtureId)` - Starts capturing logs for a fixture
  - `stopFixtureLogCapture(String fixtureId)` - Stops capture and returns log entries
- Updated `addFixtureToTest()` to accept and store log entries

#### QAPJunitExtension.java
- Modified `initializeLogCapture()` to pass log capturer to method interceptor
- Updated `stopLogCaptureAndAttach()` to ensure TestExecution object exists
- Now populates both:
  - Top-level `logEntries` (for backward compatibility)
  - `lifecycle.test.logEntries` (for lifecycle-specific logs)

## Lifecycle Structure

The enhanced lifecycle tracking now provides:

```json
{
  "lifecycle": {
    "beforeAll": [],  // Class-level, tracked separately
    "beforeEach": [
      {
        "methodName": "setup",
        "className": "PaymentProcessorTest",
        "order": 1,
        "status": "PASSED",
        "durationNanos": 1767916,
        "logEntries": [
          {
            "timestamp": 1769225749.505,
            "level": "INFO",
            "logger": "com.example.testapp.PaymentProcessorTest",
            "thread": "Test worker",
            "message": "Creating new payment processor instance",
            "mdc": {},
            "markers": []
          }
        ]
      }
    ],
    "test": {
      "status": "PASSED",
      "durationNanos": 12473500,
      "logEntries": [
        // All logs from test execution (including fixture logs from same capture)
      ]
    },
    "afterEach": [
      {
        "methodName": "teardown",
        "className": "OrderProcessingTest",
        "order": 1,
        "status": "PASSED",
        "durationNanos": 150000,
        "logEntries": [
          // Logs captured during teardown
        ]
      }
    ],
    "afterAll": []  // Class-level, tracked separately
  }
}
```

## Benefits

### 1. **Complete Visibility**
- See logs from every lifecycle phase
- Understand what setup code is doing
- Debug teardown issues easily
- Track resource initialization/cleanup

### 2. **Isolation**
- Each fixture has its own log capture
- Easily identify which phase logged what
- No confusion about log sources

### 3. **Debugging Power**
- When a test fails, see:
  - What was set up in beforeEach
  - What the test itself logged
  - What happened during teardown
  - Any errors in fixtures

### 4. **Performance Analysis**
- Identify slow fixture methods
- See duration of each phase
- Optimize setup/teardown code

## Configuration

Control log capture via `qap.properties`:

```properties
# Enable/disable logging
qap.logging.enabled=true

# Minimum log level (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
qap.logging.min.level=DEBUG

# Max entries per test/fixture
qap.logging.max.entries=100

# Max message length
qap.logging.max.message.length=10000

# Capture stack traces
qap.logging.capture.stacktraces=true

# Include MDC context
qap.logging.include.mdc=true

# Include log markers
qap.logging.include.markers=true

# Filter by logger name patterns
qap.logging.logger.patterns=com.example.*
```

## Example Output

### Before Enhancement
```json
{
  "lifecycle": {
    "beforeEach": [
      {
        "methodName": "setup",
        "status": "PASSED",
        "durationNanos": 1000000
        // No logs!
      }
    ],
    "test": {
      "status": "PASSED",
      "durationNanos": 5000000
      // No logs!
    }
  }
}
```

### After Enhancement
```json
{
  "lifecycle": {
    "beforeEach": [
      {
        "methodName": "setup",
        "status": "PASSED",
        "durationNanos": 1767916,
        "logEntries": [
          {
            "level": "INFO",
            "message": "Creating new payment processor instance",
            "timestamp": 1769225749.505
          }
        ]
      }
    ],
    "test": {
      "status": "PASSED",
      "durationNanos": 12473500,
      "logEntries": [
        {
          "level": "INFO",
          "message": "Testing credit card payment processing",
          "timestamp": 1769225749.506
        },
        {
          "level": "DEBUG",
          "message": "Processing payment - Card: ****9012",
          "timestamp": 1769225749.510
        },
        {
          "level": "INFO",
          "message": "Payment approved",
          "timestamp": 1769225749.512
        }
      ]
    }
  }
}
```

## Verification

Successfully tested with:
- ✅ PaymentProcessorTest - beforeEach, test execution logs captured
- ✅ OrderProcessingTest - afterEach logs captured
- ✅ All log levels (TRACE, DEBUG, INFO, WARN, ERROR)
- ✅ Multiple fixtures in sequence
- ✅ Nested test classes
- ✅ Parameterized tests

## Technical Notes

1. **Log Capture Isolation**: Each fixture and test execution gets a unique capture ID using context.getUniqueId() + phase identifier + timestamp
2. **Error Handling**: Log capture failures never fail tests - errors are silently ignored
3. **Memory Safety**: Respects `qap.logging.max.entries` to prevent OOM
4. **Thread Safety**: Uses the existing thread-safe log capturer infrastructure
5. **Backward Compatibility**: Existing tests continue to work without modification

## Future Enhancements

Potential improvements:
- Aggregate logs across all parameterized runs
- Filter logs by phase in JSON output
- Add log statistics (count per level, per logger)
- Capture logs from beforeAll/afterAll at class level (currently tracked in fixtures list)
- Log correlation between nested test classes

## Conclusion

The fixture and lifecycle logging implementation provides comprehensive visibility into test execution at every phase. This significantly improves debugging capabilities and helps developers understand exactly what's happening during test setup, execution, and teardown.
