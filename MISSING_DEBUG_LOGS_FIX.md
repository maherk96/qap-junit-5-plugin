# Missing DEBUG Logs Issue - Root Cause and Fix

## Problem

The DEBUG log line was missing from test execution:
```java
logger.debug("Processing payment - Card: {}, Amount: ${}", maskCard(cardNumber), amount);
```

Even though `qap.properties` was configured with:
```properties
qap.logging.min.level=DEBUG
```

## Root Cause

The `QAPJunitMethodInterceptor` was using `QAPLogCaptureConfig.defaultConfig()` for fixture and test log capture, which has a **hardcoded default of INFO level**:

```java
// In QAPLogCaptureConfig.Builder
private QAPLogLevel minLevel = QAPLogLevel.INFO;  // ❌ Hardcoded default
```

This meant:
- ✅ Properties were correctly read: `qap.logging.min.level=DEBUG`
- ❌ But fixtures/tests used hardcoded `INFO` level
- ❌ DEBUG logs were filtered out despite user configuration

## The Fix

### 1. Added Config Field to Interceptor

```java
public class QAPJunitMethodInterceptor {
  private volatile QAPLogCapturer logCapturer;
  private volatile QAPLogCaptureConfig logCaptureConfig;  // ✅ Added
  
  public void setLogCaptureConfig(QAPLogCaptureConfig config) {
    this.logCaptureConfig = config;
  }
}
```

### 2. Updated Log Capture to Use Configured Level

```java
private void startFixtureLogCapture(String fixtureId) {
  // Use configured log level from qap.properties
  QAPLogCaptureConfig config =
      (logCaptureConfig != null)
          ? logCaptureConfig  // ✅ Use user config
          : QAPLogCaptureConfig.defaultConfig();  // Fallback
  logCapturer.startCapture(fixtureId, config);
}
```

### 3. Pass Config from Extension to Interceptor

```java
private void initializeLogCapture() {
  if (capturerOpt.isPresent()) {
    logCapturer = capturerOpt.get();
    
    // Build config from qap.properties
    QAPLogCaptureConfig config = buildLogCaptureConfig();  // ✅ From properties
    
    // Pass to interceptor
    if (methodInterceptor instanceof QAPJunitMethodInterceptor) {
      ((QAPJunitMethodInterceptor) methodInterceptor).setLogCapturer(logCapturer);
      ((QAPJunitMethodInterceptor) methodInterceptor).setLogCaptureConfig(config);  // ✅ Pass config
    }
  }
}
```

## Verification

### Before Fix
```json
{
  "test": {
    "logEntries": [
      {"level": "INFO", "message": "Testing credit card payment processing"},
      // ❌ DEBUG log missing!
      {"level": "INFO", "message": "Payment approved - Transaction ID: TXN-1000"}
    ]
  }
}
```

### After Fix
```json
{
  "test": {
    "logEntries": [
      {"level": "INFO", "message": "Testing credit card payment processing"},
      {"level": "DEBUG", "message": "Processing payment - Card: ****9012, Amount: $99.99"},  // ✅ Present!
      {"level": "INFO", "message": "Payment approved - Transaction ID: TXN-1000"}
    ]
  }
}
```

## Why This Matters

### User Expectations
- Users configure `qap.logging.min.level=DEBUG` expecting DEBUG logs
- Configuration should be respected consistently across all capture points

### Consistency
- Test execution now uses the **same config** for:
  - BeforeAll fixtures
  - BeforeEach fixtures
  - Test execution
  - AfterEach fixtures
  - AfterAll fixtures

### Debugging Power
- DEBUG logs often contain critical diagnostic information
- Missing them defeats the purpose of comprehensive log capture
- Example: Card masking, request IDs, intermediate calculations

## Configuration Hierarchy

Now the log level configuration works correctly:

```
1. qap.properties (highest priority)
   └─> qap.logging.min.level=DEBUG
       
2. buildLogCaptureConfig() reads properties
   └─> QAPLogCaptureConfig with minLevel=DEBUG
       
3. Passed to QAPJunitMethodInterceptor
   └─> setLogCaptureConfig(config)
       
4. Used for ALL log captures
   ├─ BeforeAll fixtures  ✅ DEBUG
   ├─ BeforeEach fixtures ✅ DEBUG
   ├─ Test execution      ✅ DEBUG
   ├─ AfterEach fixtures  ✅ DEBUG
   └─ AfterAll fixtures   ✅ DEBUG
```

## Testing

Verified with `testProcessCreditCardPayment`:
- ✅ BeforeEach logs (INFO): "Creating new payment processor instance"
- ✅ Test INFO log: "Testing credit card payment processing"
- ✅ Test DEBUG log: "Processing payment - Card: ****9012, Amount: $99.99"
- ✅ Test INFO log: "Payment approved - Transaction ID: TXN-1000"

All log levels now respect the `qap.properties` configuration! 🎉

## Lessons Learned

1. **Avoid Hardcoded Defaults**: Use configuration system consistently
2. **Test All Log Levels**: Not just INFO, but DEBUG/TRACE too
3. **Config Propagation**: Ensure config reaches all capture points
4. **User Expectations**: Honor configured settings across the board

## Related Configuration

All these properties now work consistently:
```properties
qap.logging.min.level=DEBUG           # ✅ Respected
qap.logging.max.entries=100           # ✅ Respected
qap.logging.max.message.length=10000  # ✅ Respected
qap.logging.logger.patterns=...       # ✅ Respected
```
