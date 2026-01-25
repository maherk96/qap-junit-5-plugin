# Failure Location - User Code Frame Extraction

## Summary

Updated `failure.location` to point to the **first user-code stack frame** instead of JUnit internals, making it easier for UIs to link directly to the failing test method.

## Changes Made

### Modified: `ExceptionFormatter.java`

#### Before
```java
private static QAPFailureLocation extractLocation(Throwable throwable) {
  StackTraceElement firstElement = stackTrace[0]; // ❌ Points to JUnit internals
  return new QAPFailureLocation(className, methodName, fileName, lineNumber);
}
```

**Result**: `failure.location` pointed to `AssertionFailureBuilder.build()` (JUnit internal)

```json
{
  "location": {
    "class": "org.junit.jupiter.api.AssertionFailureBuilder",
    "method": "build",
    "file": "AssertionFailureBuilder.java",
    "line": 151
  }
}
```

#### After
```java
private static QAPFailureLocation extractLocation(Throwable throwable) {
  // Find the first user-code frame (skip JUnit, java.base, org.gradle, etc.)
  for (StackTraceElement element : stackTrace) {
    if (!isFrameworkClass(element.getClassName())) {
      userFrame = element; // ✅ First user code frame
      break;
    }
  }
  return new QAPFailureLocation(...);
}

private static boolean isFrameworkClass(String className) {
  return className.startsWith("org.junit.")
      || className.startsWith("org.opentest4j.")
      || className.startsWith("java.base/")
      || className.startsWith("jdk.internal.")
      || className.startsWith("java.lang.reflect.")
      || className.startsWith("org.gradle.")
      || className.startsWith("worker.org.gradle.")
      || className.startsWith("jdk.proxy")
      || className.startsWith("com.mk.fx.qa.qap.junit.extension.");
}
```

**Result**: `failure.location` points to the actual test method

```json
{
  "location": {
    "class": "com.example.testapp.PaymentProcessorTest",
    "method": "testMinimumPaymentAmount",
    "file": "PaymentProcessorTest.java",
    "line": 116
  }
}
```

## Benefits

1. **Better UI Integration**: UIs can directly link to the failing test method line
2. **Faster Debugging**: Developers see exactly where their test failed, not framework internals
3. **Stack Trace Still Complete**: Full stack trace remains available in `failure.stackTrace[]`
4. **Fallback Handling**: If no user-code frame is found, falls back to first element

## Test Results

### Failed Test
```bash
./gradlew :test-app:test --tests "...testMinimumPaymentAmount"
```

**Location extracted**:
```json
{
  "class": "com.example.testapp.PaymentProcessorTest",
  "method": "testMinimumPaymentAmount",
  "file": "PaymentProcessorTest.java",
  "line": 116
}
```
✅ Points to user code (`PaymentProcessorTest:116`)

### Passing Test
```bash
./gradlew :test-app:test --tests "...testProcessCreditCardPayment"
```

**Result**: `{"status": "PASSED", "hasFailure": false}` ✅

## Framework Classes Skipped

The following patterns are skipped when searching for user-code frames:
- `org.junit.*` - JUnit framework
- `org.opentest4j.*` - OpenTest4J assertions
- `java.base/*` - Java base libraries
- `jdk.internal.*` - JDK internals
- `java.lang.reflect.*` - Reflection API
- `org.gradle.*` - Gradle test runner
- `worker.org.gradle.*` - Gradle worker processes
- `jdk.proxy*` - JDK proxies
- `com.mk.fx.qa.qap.junit.extension.*` - Our own extension code

## Implementation Details

### Algorithm
1. Iterate through stack trace elements from top to bottom
2. Check each frame's class name against framework patterns
3. Return first frame that doesn't match framework patterns
4. Fallback to first element if all frames are framework code (unlikely)

### Edge Cases Handled
- Empty stack traces → returns `null`
- All framework frames → falls back to first element
- Line number unavailable (`-1`) → set to `null` in JSON
