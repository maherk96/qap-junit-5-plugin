# Standardized ClassName to FQN (Fully Qualified Names)

## Change Summary

Standardized all lifecycle hook `className` fields to use **Fully Qualified Names (FQN)** for consistency and clarity.

## Before

### Inconsistent Naming
```json
{
  "fixtures": [{
    "className": "com.example.testapp.PaymentProcessorTest"  // ✅ FQN
  }],
  "testCases": [{
    "lifecycle": {
      "beforeEach": [{
        "className": "PaymentProcessorTest"  // ❌ Simple name
      }]
    }
  }]
}
```

**Problems:**
- Inconsistent between class-level and test-level fixtures
- Simple names can be ambiguous (multiple classes with same name in different packages)
- Harder to trace back to source code
- Different standards for same conceptual field

## After

### Consistent FQN Throughout
```json
{
  "fixtures": [{
    "className": "com.example.testapp.PaymentProcessorTest"  // ✅ FQN
  }],
  "testCases": [{
    "lifecycle": {
      "beforeEach": [{
        "className": "com.example.testapp.PaymentProcessorTest"  // ✅ FQN
      }]
    }
  }]
}
```

**Benefits:**
- ✅ Consistent naming across all fixture types
- ✅ No ambiguity - FQN is globally unique
- ✅ Easy to locate source code
- ✅ Better for tooling and IDE integration
- ✅ Supports test inheritance scenarios

## Changes Made

### 1. Updated Model Documentation

**QAPTestFixture.java:**
```java
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPTestFixture {
  private final String methodName;
  private final String className; // Fully qualified: e.g., "com.example.testapp.PaymentProcessorTest"
  // ...
}
```

### 2. Updated Interceptor Implementation

**QAPJunitMethodInterceptor.java:**
```java
private void addFixtureToTest(...) {
  String methodName = fixtureMethod.getName();
  String className = fixtureMethod.getDeclaringClass().getName(); // ✅ Use FQN
  
  QAPTestFixture testFixture = 
      new QAPTestFixture(methodName, className, order, status, durationNanos, error, logs);
}
```

**Previously used:**
```java
String className = fixtureMethod.getDeclaringClass().getSimpleName(); // ❌ Simple name
```

## Verification

### Complete Lifecycle Output

```json
{
  "testClasses": [{
    "className": "PaymentProcessorTest",
    "fixtures": [
      {
        "phase": "BEFORE_ALL",
        "methodName": "setupClass",
        "className": "com.example.testapp.PaymentProcessorTest",  // ✅ FQN
        "status": "PASSED"
      },
      {
        "phase": "AFTER_ALL",
        "methodName": "teardownClass",
        "className": "com.example.testapp.PaymentProcessorTest",  // ✅ FQN
        "status": "PASSED"
      }
    ],
    "testCases": [{
      "methodName": "testProcessCreditCardPayment",
      "lifecycle": {
        "beforeEach": [{
          "methodName": "setup",
          "className": "com.example.testapp.PaymentProcessorTest",  // ✅ FQN
          "order": 1,
          "status": "PASSED"
        }],
        "test": {
          "durationNanos": 12323000
        },
        "afterEach": []
      }
    }]
  }]
}
```

## Use Cases Where FQN Matters

### 1. Test Inheritance
```java
package com.example.testapp.base;
public abstract class BaseTest {
  @BeforeEach
  void baseSetup() { ... }
}

package com.example.testapp;
public class PaymentProcessorTest extends BaseTest {
  @BeforeEach
  void setup() { ... }
}
```

With FQN, you can clearly see:
```json
{
  "beforeEach": [
    {
      "methodName": "baseSetup",
      "className": "com.example.testapp.base.BaseTest"  // ✅ Clear origin
    },
    {
      "methodName": "setup",
      "className": "com.example.testapp.PaymentProcessorTest"  // ✅ Clear origin
    }
  ]
}
```

### 2. Nested Test Classes
```java
package com.example.testapp;
public class PaymentProcessorTest {
  @Nested
  class RefundTests {
    @BeforeEach
    void setupRefundTest() { ... }
  }
}
```

With FQN:
```json
{
  "beforeEach": [{
    "methodName": "setupRefundTest",
    "className": "com.example.testapp.PaymentProcessorTest$RefundTests"  // ✅ Includes nesting
  }]
}
```

### 3. Name Collisions
```java
package com.example.payment;
public class PaymentProcessorTest { ... }

package com.example.billing;
public class PaymentProcessorTest { ... }  // Same name!
```

With FQN, no ambiguity:
```json
[
  {"className": "com.example.payment.PaymentProcessorTest"},
  {"className": "com.example.billing.PaymentProcessorTest"}
]
```

## Consistency Table

| Location | Field | Format | Example |
|----------|-------|--------|---------|
| Class-level fixtures | `className` | FQN | `com.example.testapp.PaymentProcessorTest` |
| Test-level beforeEach | `className` | FQN | `com.example.testapp.PaymentProcessorTest` |
| Test-level afterEach | `className` | FQN | `com.example.testapp.PaymentProcessorTest` |
| Test class metadata | `classFqn` | FQN | `com.example.testapp.PaymentProcessorTest` |
| Test class metadata | `classSimpleName` | Simple | `PaymentProcessorTest` |

## Benefits

### 1. **Consistency**
- Same field (`className`) has same format everywhere
- No need to remember which uses FQN vs simple name

### 2. **Clarity**
- Instantly know the package and full path to source
- No guessing which `PaymentProcessorTest` when multiple exist

### 3. **Tooling Support**
- IDEs can create direct links from FQN to source code
- CI/CD tools can map to file paths easily
- Test reporting dashboards can group by package

### 4. **Future-Proof**
- Supports complex test hierarchies
- Works with test mixins and inheritance
- Compatible with nested test classes

### 5. **Machine-Readable**
- Parsers can extract package structure
- Can build package-level reports
- Easy to filter by package prefix

## Migration Notes

If existing tools depend on simple class names:

**Extract simple name from FQN:**
```javascript
// JavaScript
const fqn = "com.example.testapp.PaymentProcessorTest";
const simpleName = fqn.split('.').pop();  // "PaymentProcessorTest"

// Python
fqn = "com.example.testapp.PaymentProcessorTest"
simple_name = fqn.split('.')[-1]  # "PaymentProcessorTest"

// Java
String fqn = "com.example.testapp.PaymentProcessorTest";
String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);  // "PaymentProcessorTest"
```

**Extract package:**
```javascript
const fqn = "com.example.testapp.PaymentProcessorTest";
const pkg = fqn.substring(0, fqn.lastIndexOf('.'));  // "com.example.testapp"
```

## Conclusion

All lifecycle hooks now use FQN for `className` field, providing:
- ✅ Consistency across all fixture types
- ✅ Clarity for test inheritance scenarios
- ✅ Better tooling integration
- ✅ Future-proof for complex test structures
- ✅ Machine-readable package information

This standardization makes the JSON more robust and easier to work with! 🎉
