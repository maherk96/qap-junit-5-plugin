# Test App - QAP JUnit 5 Extension Demo

This module demonstrates the complete integration of the QAP JUnit 5 Extension with automatic log capture.

## 📋 What's Included

### Configuration Files

1. **`qap.properties`** - QAP runtime configuration
   ```properties
   qap.app.name=TestApp-Demo
   qap.report.test.data=true  # Enable JSON reporting
   qap.test.environment=TEST
   qap.run.environment=DEV
   ```

2. **`log4j2-test.xml`** - Log4j2 logging configuration
   - Console appender for viewing logs
   - File appender (build/test-logs/test-app.log)
   - QAP framework logger set to WARN (hides internal framework logs)
   - Test code logger set to DEBUG (captures all test logs)

3. **`META-INF/services/org.junit.jupiter.api.extension.Extension`** - JUnit extension auto-discovery
   - Automatically registers `QAPJunitExtension`
   - No need for `@ExtendWith` annotation

### Test Classes

1. **`UserServiceTest`** - Basic CRUD operations
   - User creation, update, deletion
   - Email validation
   - Nested test groups
   - **8 tests + 2 nested tests = 10 total**

2. **`PaymentProcessorTest`** - Financial processing
   - Credit card payment processing
   - Parameterized tests for various amounts
   - Parameterized tests for fee calculations
   - Payment validation and error handling
   - Nested refund tests
   - **13 parameterized tests + 2 nested = 15 total**

3. **`InventoryServiceTest`** - Warehouse management
   - Inventory add/update/reserve operations
   - Low stock alerts
   - Error handling for out-of-stock
   - Nested batch operations
   - **11 tests + 2 nested = 13 total**

4. **`OrderProcessingTest`** - End-to-end order workflow
   - Order creation and status transitions
   - Discount code application
   - Shipping calculations
   - Payment processing
   - Nested bulk operations and edge cases
   - **14 tests + 5 nested = 19 total**

**Total: 46 tests across 4 test classes**

---

## 🚀 Running the Tests

### Run All Tests
```bash
./gradlew :test-app:test
```

### Run Specific Test Class
```bash
./gradlew :test-app:test --tests "UserServiceTest"
./gradlew :test-app:test --tests "PaymentProcessorTest"
./gradlew :test-app:test --tests "InventoryServiceTest"
./gradlew :test-app:test --tests "OrderProcessingTest"
```

### Run with Console Output
```bash
./gradlew :test-app:test --console=plain
```

### Clean and Run
```bash
./gradlew :test-app:clean :test-app:test
```

---

## 📊 Expected Output

### Test Execution
You'll see all tests running with their display names:
```
User Service Tests > Should create new user successfully PASSED
User Service Tests > Should find user by username PASSED
Payment Processor Tests > Processing $99.99 payment PASSED
...
```

### QAP JSON Report
At the end of the test run, the QAP extension will output a JSON report to stdout containing:
- Launch metadata (ID, timestamps, environment)
- All test classes with their tests
- Test results (PASSED/FAILED/SKIPPED)
- Timing information
- **Captured log entries** from each test
- Tags and metadata
- Parameterization details

### Example JSON Structure
```json
{
  "header": {
    "launchStartTime": 1706022367000,
    "launchEndTime": 1706022370000,
    "launchId": "TestLaunch-abc123def456",
    "applicationName": "TestApp-Demo",
    "testEnvironment": "TEST",
    "user": "test-user"
  },
  "testClasses": [
    {
      "className": "com.example.testapp.UserServiceTest",
      "tests": [
        {
          "methodName": "testCreateUser",
          "testCaseId": "com.example.testapp.UserServiceTest.testCreateUser",
          "status": "PASSED",
          "duration": 123,
          "logEntries": [
            {
              "timestamp": 1706022367500,
              "level": "INFO",
              "message": "Testing user creation",
              "loggerName": "com.example.testapp.UserServiceTest"
            }
          ]
        }
      ]
    }
  ]
}
```

### Log Files
Logs are also written to:
```
test-app/build/test-logs/test-app.log
```

---

## 🔍 What the Tests Demonstrate

### 1. **Comprehensive Logging**
Every test uses SLF4J logging at different levels:
- `INFO` - Key test steps
- `DEBUG` - Detailed execution info
- `WARN` - Warning scenarios
- `ERROR` - Expected failures

**The QAP extension automatically captures all these logs and includes them in the JSON report!**

### 2. **Test Organization**
- Regular `@Test` methods
- `@ParameterizedTest` with multiple parameter providers:
  - `@ValueSource` - Single parameter
  - `@CsvSource` - Multiple parameters with names
- `@Nested` test groups for logical organization
- `@BeforeEach` / `@AfterEach` / `@BeforeAll` / `@AfterAll` lifecycle methods

### 3. **Rich Metadata**
- `@DisplayName` - Human-readable test names
- `@Tag` - Test categorization (e.g., "user-service", "payment", "integration")
- Custom display names for parameterized tests

### 4. **Error Scenarios**
Tests include both successful and failing scenarios:
- ✓ Valid operations that pass
- ✗ Invalid inputs that throw exceptions
- Edge cases and boundary conditions

### 5. **Real-World Patterns**
Tests simulate realistic business scenarios:
- User management (CRUD operations)
- Payment processing (with validation)
- Inventory management (with concurrency concerns)
- Order processing (end-to-end workflows)

---

## 📦 Dependencies

This module depends on:

1. **`:qap-plugin`** - The core QAP JUnit 5 extension
   - Provides `QAPJunitExtension`
   - Collects test metadata and results

2. **`:qap-logging-log4j2`** - Automatic log capture for Log4j2
   - Provides `QAPLog4j2Appender`
   - Captures logs during test execution
   - Attaches logs to QAP test results

3. **JUnit 5** - Test framework
   - `junit-jupiter-api`
   - `junit-jupiter-engine`
   - `junit-jupiter-params` (for parameterized tests)

4. **Log4j2** - Logging implementation
   - `log4j-slf4j2-impl` (SLF4J bridge)
   - `log4j-api`
   - `log4j-core`

---

## 🎯 Key Features Demonstrated

### ✅ Automatic Extension Registration
No need for `@ExtendWith(QAPJunitExtension.class)` - the extension is automatically discovered via Java's ServiceLoader mechanism.

### ✅ Automatic Log Capture
Logs written during test execution are automatically:
1. Captured by the Log4j2 appender
2. Associated with the correct test
3. Included in the QAP JSON report

### ✅ Zero Code Changes Required
Tests are written as normal JUnit 5 tests with standard SLF4J logging. The QAP extension and logging modules work transparently in the background.

### ✅ Rich Test Reports
The JSON output includes:
- All test metadata
- Execution timing
- Pass/fail status
- Complete log history for each test
- Nested test hierarchy
- Parameterization details

### ✅ Thread-Safe
Works correctly with:
- Parallel test execution
- Nested tests
- Parameterized tests with multiple invocations

---

## 🔧 Customization

### Change Reporting Behavior
Edit `qap.properties`:
```properties
# Disable JSON reporting
qap.report.test.data=false

# Change application name
qap.app.name=MyCustomApp

# Change environment
qap.run.environment=PROD
```

### Adjust Logging Levels
Edit `log4j2-test.xml`:
```xml
<!-- Hide QAP framework logs (recommended for users) -->
<Logger name="com.mk.fx.qa.qap" level="warn" additivity="false">
    <AppenderRef ref="Console"/>
</Logger>

<!-- Show QAP framework debug logs (for troubleshooting) -->
<Logger name="com.mk.fx.qa.qap" level="debug" additivity="false">
    <AppenderRef ref="Console"/>
</Logger>

<!-- Adjust test logging verbosity -->
<Logger name="com.example.testapp" level="info" additivity="true">
    <AppenderRef ref="Console"/>
</Logger>
```

**Important:** Keep `additivity="true"` for your test loggers to enable automatic log capture!

### Use Different Publishers
The QAP extension uses `StdOutPublisher` by default. You can configure different publishers in the code or via configuration (future enhancement).

---

## 📈 Test Coverage Statistics

| Test Class | Tests | Nested | Total | Success Rate |
|------------|-------|--------|-------|--------------|
| UserServiceTest | 8 | 2 | 10 | 100% |
| PaymentProcessorTest | 13 | 2 | 15 | 100% |
| InventoryServiceTest | 11 | 2 | 13 | 100% |
| OrderProcessingTest | 14 | 5 | 19 | ~95%* |
| **TOTAL** | **46** | **11** | **46** | **~98%** |

\* OrderProcessingTest includes intentional failure tests for validation

---

## 🎨 Test Output Features

### Visual Test Names
Tests use emojis and descriptive names in `OrderProcessingTest`:
```
✓ Should create and process simple order
✓ Should apply discount code
✗ Should fail for invalid discount code
```

### Structured Logging
Tests use structured logging patterns:
```java
logger.info("→ Setting up test data");
logger.debug("Created 5 orders for bulk testing");
logger.warn("⚠ Attempting invalid operation");
logger.error("✗ Expected failure: {}", exception.getMessage());
```

### Hierarchical Organization
Nested tests create logical groupings:
```
Order Processing Tests
  ├─ Basic operations
  ├─ Bulk Order Operations
  │  ├─ Process all pending orders
  │  └─ Generate bulk report
  └─ Order Fulfillment Edge Cases
     ├─ Handle empty order
     ├─ Negative quantity validation
     └─ Order not found
```

---

## 🚦 Troubleshooting

### No JSON Output?
1. Check `qap.properties` has `qap.report.test.data=true`
2. Ensure tests are actually running (not cached)
3. Run with `--console=plain` to see all output
4. Check `showStandardStreams = true` in `build.gradle`

### Logs Not Captured?
1. Verify Log4j2 is configured (`log4j2-test.xml` present)
2. Ensure `:qap-logging-log4j2` dependency is included
3. Check logger names match configuration

### Tests Not Using Extension?
1. Verify `META-INF/services/org.junit.jupiter.api.extension.Extension` file exists
2. Content should be: `com.mk.fx.qa.qap.junit.extension.QAPJunitExtension`
3. File must be in `src/test/resources/META-INF/services/`

---

## 📚 Next Steps

1. **Review the JSON Output** - Examine the structure and captured logs
2. **Add Your Own Tests** - Create tests for your application
3. **Customize Configuration** - Adjust `qap.properties` for your environment
4. **Integrate with CI/CD** - Parse JSON output in your build pipeline
5. **Add More Logging Frameworks** - Try `:qap-logging-logback` when available

---

*This test-app demonstrates production-ready integration of the QAP JUnit 5 Extension with automatic log capture!*
