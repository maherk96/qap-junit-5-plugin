# Property-Based Logging Configuration ⭐

**Date:** January 23, 2026  
**Enhancement:** User-friendly logging configuration via `qap.properties`

---

## Overview

Previously, users had to extend `QAPJunitExtension` and override methods to customize log capture behavior. Now they can simply add properties to `qap.properties` - **zero code changes required!**

---

## ✅ What Changed

### Before (Required Code Changes)

```java
// Users had to create a custom extension class
public class CustomQAPExtension extends QAPJunitExtension {
    @Override
    protected QAPLogCaptureConfig getLogCaptureConfig() {
        return QAPLogCaptureConfig.builder()
            .minLevel(QAPLogLevel.DEBUG)
            .maxEntriesPerTest(5000)
            .addLoggerPattern("com.myapp.*")
            .build();
    }
}
```

Then register it in `META-INF/services/org.junit.jupiter.api.extension.Extension`

❌ **Problems:**
- Requires Java code changes
- Requires recompilation
- Not discoverable
- More complex for users

### After (Simple Properties) ⭐

```properties
# Just add to qap.properties - NO code changes!
qap.logging.min.level=DEBUG
qap.logging.max.entries=5000
qap.logging.logger.patterns=com.myapp.*
```

✅ **Benefits:**
- Zero code changes
- No recompilation needed
- Easy to discover (all in one file)
- Consistent with existing QAP configuration

---

## 📝 Available Properties

### Complete Property List

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `qap.logging.enabled` | boolean | `true` | Enable/disable log capture |
| `qap.logging.min.level` | enum | `DEBUG` | Minimum log level: TRACE, DEBUG, INFO, WARN, ERROR, FATAL |
| `qap.logging.max.entries` | int | `1000` | Maximum log entries per test |
| `qap.logging.max.message.length` | int | `10000` | Maximum characters per log message |
| `qap.logging.capture.stacktraces` | boolean | `true` | Capture exception stack traces |
| `qap.logging.include.mdc` | boolean | `true` | Capture MDC/ThreadContext |
| `qap.logging.include.markers` | boolean | `true` | Capture SLF4J/Log4j2 markers |
| `qap.logging.logger.patterns` | string | `""` (all) | Comma-separated logger patterns (wildcards supported) |

---

## 🎯 Common Use Cases

### Use Case 1: Quieter Logging (WARN+ Only)

**Problem:** Too many logs in reports  
**Solution:**

```properties
qap.logging.min.level=WARN
```

**Result:** Only captures WARN, ERROR, FATAL logs

---

### Use Case 2: Application Logs Only

**Problem:** Third-party library logs cluttering reports  
**Solution:**

```properties
qap.logging.logger.patterns=com.mycompany.*
```

**Result:** Only captures logs from `com.mycompany` packages

---

### Use Case 3: Multiple Packages

**Problem:** Need logs from multiple specific packages  
**Solution:**

```properties
qap.logging.logger.patterns=com.myapp.*,org.springframework.web.*,com.mycompany.critical.*
```

**Result:** Captures logs from specified packages only

---

### Use Case 4: High-Volume Tests

**Problem:** Tests generate thousands of log entries  
**Solution:**

```properties
qap.logging.max.entries=5000
qap.logging.max.message.length=20000
```

**Result:** Higher limits allow more logs to be captured

---

### Use Case 5: Minimal JSON Size

**Problem:** JSON reports are too large  
**Solution:**

```properties
qap.logging.min.level=WARN
qap.logging.include.mdc=false
qap.logging.include.markers=false
qap.logging.capture.stacktraces=false
qap.logging.max.message.length=500
```

**Result:** Smaller JSON files with only essential information

---

### Use Case 6: Disable Logging Entirely

**Problem:** Don't want log capture for this project  
**Solution:**

```properties
qap.logging.enabled=false
```

**Result:** No logs captured, smaller reports, faster execution

---

## 📁 Example `qap.properties`

### Minimal Configuration (Uses Defaults)

```properties
# QAP Test Configuration
qap.app.name=MyApp
qap.user=test-user
qap.test.environment=TEST

# That's it! Logging uses defaults
```

### Comprehensive Configuration

```properties
# ========================================
# QAP Test Configuration
# ========================================
qap.app.name=MyApp
qap.report.fix.messaging=true
qap.user=test-user
qap.test.environment=TEST
qap.run.environment=DEV
qap.report.test.data=true
qap.api.key=my-api-key

# ========================================
# QAP Log Capture Configuration
# ========================================

# Enable/disable log capture (default: true)
qap.logging.enabled=true

# Minimum log level: TRACE, DEBUG, INFO, WARN, ERROR, FATAL
# Default: DEBUG (captures everything except TRACE)
qap.logging.min.level=DEBUG

# Maximum number of log entries to capture per test (prevents OOM)
# Default: 1000
qap.logging.max.entries=1000

# Maximum length of each log message in characters
# Default: 10000
qap.logging.max.message.length=10000

# Capture exception stack traces in logs (default: true)
qap.logging.capture.stacktraces=true

# Include MDC (Mapped Diagnostic Context) in captured logs (default: true)
qap.logging.include.mdc=true

# Include SLF4J/Log4j2 markers in captured logs (default: true)
qap.logging.include.markers=true

# Logger name patterns to capture (comma-separated, supports wildcards)
# Empty = capture all loggers (default)
# Example: com.example.*,org.springframework.web.*
qap.logging.logger.patterns=
```

---

## 🔍 How It Works

### Implementation Flow

```
Test Starts
    ↓
QAPJunitExtension.beforeEach()
    ↓
buildLogCaptureConfig()  ← NEW METHOD
    ↓
Reads qap.properties via QAPPropertiesLoader
    ↓
Builds QAPLogCaptureConfig from properties
    ↓
logCapturer.startCapture(testId, config)
    ↓
Logs captured with user's configuration
```

### Code Changes

#### 1. Enhanced `QAPPropertiesLoader`

Added helper methods to read properties:

```java
public String getProperty(String key, String defaultValue)
public boolean getBooleanProperty(String key, boolean defaultValue)
public int getIntProperty(String key, int defaultValue)
```

#### 2. New Method in `QAPJunitExtension`

```java
private QAPLogCaptureConfig buildLogCaptureConfig() {
    QAPPropertiesLoader props = runtime.getPropertiesLoader();
    
    // Read all logging properties with defaults
    boolean enabled = props.getBooleanProperty("qap.logging.enabled", true);
    String minLevel = props.getProperty("qap.logging.min.level", "DEBUG");
    int maxEntries = props.getIntProperty("qap.logging.max.entries", 1000);
    // ... etc
    
    return QAPLogCaptureConfig.builder()
        .enabled(enabled)
        .minLevel(parseLevel(minLevel))
        .maxEntriesPerTest(maxEntries)
        // ... etc
        .build();
}
```

#### 3. Updated `startLogCapture()`

```java
private void startLogCapture(ExtensionContext context) {
    // OLD: Hard-coded config
    // QAPLogCaptureConfig config = QAPLogCaptureConfig.builder()
    //     .minLevel(QAPLogLevel.DEBUG)
    //     .build();
    
    // NEW: Build from properties
    QAPLogCaptureConfig config = buildLogCaptureConfig();
    
    logCapturer.startCapture(testId, config);
}
```

---

## ✅ Benefits

### For Users

1. **No Code Required** - Just edit properties
2. **No Recompilation** - Change behavior without rebuilding
3. **Easy Discovery** - All configuration in one place
4. **Environment-Specific** - Different properties per environment
5. **Git-Friendly** - Easy to version control and review

### For Developers

1. **Consistent Pattern** - Matches existing QAP property usage
2. **Backward Compatible** - Defaults work for existing users
3. **Type-Safe** - Proper parsing with defaults on error
4. **Well-Documented** - Clear property names and examples

---

## 🧪 Testing

### Property Validation

The implementation includes:

- **Default Values** - Missing properties use sensible defaults
- **Error Handling** - Invalid values log warnings and use defaults
- **Type Conversion** - Proper parsing of booleans, integers, enums
- **Pattern Parsing** - Comma-separated patterns with trim

### Example Error Handling

```properties
# Invalid level - logs warning, uses DEBUG
qap.logging.min.level=INVALID

# Invalid integer - logs warning, uses 1000
qap.logging.max.entries=not-a-number
```

Logs produced:
```
WARN: Invalid qap.logging.min.level 'INVALID', using DEBUG. Valid: TRACE, DEBUG, INFO, WARN, ERROR, FATAL
WARN: Invalid integer value for qap.logging.max.entries: 'not-a-number', using default: 1000
```

---

## 📚 Documentation Updates

### Updated Files

1. **`qap-logging-log4j2/README.md`**
   - Added "Property-Based Configuration" section
   - Moved programmatic config to "Advanced" section
   - Added common configuration examples

2. **`qap-logging-logback/README.md`**
   - Same updates as Log4j2 README
   - Consistent documentation across both plugins

3. **`test-app/src/test/resources/qap.properties`**
   - Added comprehensive logging property examples
   - Added comments explaining each property
   - Added example configurations for common use cases

---

## 🚀 Migration Guide

### For Existing Users

**No migration needed!** Existing code continues to work with defaults.

**Optional:** Add properties to customize behavior:

```properties
# Start with this to see all logs
qap.logging.min.level=DEBUG

# Adjust as needed based on your tests
```

### For Users with Custom Extensions

If you previously extended `QAPJunitExtension`, you can now **simplify** your code:

**Before:**
```java
public class CustomQAPExtension extends QAPJunitExtension {
    @Override
    protected QAPLogCaptureConfig getLogCaptureConfig() {
        return QAPLogCaptureConfig.builder()
            .minLevel(QAPLogLevel.WARN)
            .maxEntriesPerTest(5000)
            .build();
    }
}
```

**After:**
```properties
# Just use properties!
qap.logging.min.level=WARN
qap.logging.max.entries=5000
```

Then **delete** your custom extension class and registration!

---

## 🎯 Summary

This enhancement makes QAP logging configuration:

✅ **User-Friendly** - No coding required  
✅ **Discoverable** - All in `qap.properties`  
✅ **Flexible** - Environment-specific configs  
✅ **Consistent** - Matches QAP's existing patterns  
✅ **Backward Compatible** - Existing code works  
✅ **Well-Documented** - Clear examples and guides

---

**Status:** ✅ Production Ready  
**Applies To:** Both Log4j2 and Logback plugins  
**Breaking Changes:** None (fully backward compatible)
