# QAP Properties Configuration Guide

## Overview

QAP properties allow you to configure test reporting behavior, log capture, and stack trace formatting through a simple properties file. Configuration is loaded at test runtime and applied automatically.

## How It Works

1. **Create** `qap.properties` in `src/test/resources/`
2. **Set** properties using `key=value` format
3. **Run** tests - QAP loads and applies configuration automatically
4. **Generate** JSON reports with your custom settings

## Quick Start

**Minimal configuration:**
```properties
# src/test/resources/qap.properties
qap.app.name=MyApp
qap.reporting.enabled=true
```

That's it! QAP will use defaults for everything else.

## Configuration Categories

### 1. Application & Environment

```properties
# Application identification
qap.app.name=MyApp                    # Your application name
qap.test.environment=TEST             # TEST, UAT, PROD, etc.
qap.run.environment=DEV               # Where tests are running
qap.user=john.doe                     # Test executor (default: system user)
```

### 2. Reporting

```properties
# Enable/disable JSON report generation
qap.reporting.enabled=true            # default: true

# API integration (optional)
qap.api.key=your-api-key-here         # For uploading to QAP server
```

### 3. Log Capture

Control what logs are captured during test execution:

```properties
# Enable/disable log capture
qap.logging.enabled=true              # default: true

# Minimum log level to capture
qap.logging.min.level=DEBUG           # TRACE, DEBUG, INFO, WARN, ERROR, FATAL
                                      # default: DEBUG

# Limits (prevent OOM)
qap.logging.max.entries=1000          # Max log entries per test (default: 1000)
qap.logging.max.message.length=10000  # Truncate long messages (default: 10000)

# What to include
qap.logging.capture.stacktraces=true  # Include exception traces (default: true)
qap.logging.include.mdc=true          # Include MDC context (default: true)
qap.logging.include.markers=true      # Include SLF4J markers (default: true)

# Filter by logger name (optional)
qap.logging.logger.patterns=          # Empty = capture all
# Examples:
# qap.logging.logger.patterns=com.example.*
# qap.logging.logger.patterns=com.example.*,org.springframework.web.*
```

### 4. Stack Trace Formatting

Control how exception stack traces are captured in failure reports:

```properties
# Maximum total lines (use -1 for unlimited)
qap.stacktrace.max.lines=200          # default: 200

# Head + Tail strategy (default)
qap.stacktrace.head.lines=50          # Keep first N lines (default: 50)
qap.stacktrace.tail.lines=20          # Keep last N lines (default: 20)

# Alternative: User-code only strategy
qap.stacktrace.keep.until.framework.exit=false  # default: false
# Set to true to stop at framework boundary (smaller traces)
```

## Stack Trace Strategies

### Strategy 1: Head + Tail (Default)
```properties
qap.stacktrace.keep.until.framework.exit=false
qap.stacktrace.head.lines=50
qap.stacktrace.tail.lines=20
```
**Result:** First 50 + last 20 lines with separator in between  
**Use case:** Balanced - shows error location AND test entry point

### Strategy 2: User-Code Only
```properties
qap.stacktrace.keep.until.framework.exit=true
qap.stacktrace.max.lines=100
```
**Result:** Stops when exiting user code (ignores framework tail)  
**Use case:** Minimal payloads - only YOUR code execution path

## Common Configurations

### Development (Verbose)
```properties
qap.app.name=MyApp
qap.test.environment=DEV
qap.reporting.enabled=true

# Capture everything
qap.logging.enabled=true
qap.logging.min.level=DEBUG
qap.logging.max.entries=5000

# Full stack traces
qap.stacktrace.max.lines=-1           # Unlimited
```

### CI/CD (Balanced)
```properties
qap.app.name=MyApp
qap.test.environment=TEST
qap.reporting.enabled=true

# Standard logging
qap.logging.enabled=true
qap.logging.min.level=INFO
qap.logging.max.entries=1000

# Balanced stack traces
qap.stacktrace.max.lines=200
qap.stacktrace.head.lines=50
qap.stacktrace.tail.lines=20
```

### Production Tests (Minimal)
```properties
qap.app.name=MyApp
qap.test.environment=PROD
qap.reporting.enabled=true

# Only warnings and errors
qap.logging.enabled=true
qap.logging.min.level=WARN
qap.logging.max.entries=500

# Compact stack traces
qap.stacktrace.max.lines=50
qap.stacktrace.head.lines=20
qap.stacktrace.tail.lines=10
```

### High-Volume Testing (Compact)
```properties
qap.app.name=MyApp
qap.reporting.enabled=true

# Minimal logging
qap.logging.enabled=true
qap.logging.min.level=ERROR
qap.logging.max.entries=100
qap.logging.include.mdc=false
qap.logging.include.markers=false

# User-code only stack traces
qap.stacktrace.keep.until.framework.exit=true
qap.stacktrace.max.lines=30
```

## Property Resolution

QAP loads properties in this order (later values override earlier):

1. **Built-in defaults** (hardcoded in QAP)
2. **qap.properties** file (your configuration)
3. **System properties** (JVM `-D` flags)

Example:
```bash
# Override via command line
./gradlew test -Dqap.logging.min.level=ERROR
```

## Default Values

If you don't specify a property, QAP uses these defaults:

```properties
# Application
qap.app.name=null
qap.test.environment=null
qap.run.environment=UAT
qap.user=<system-user>
qap.reporting.enabled=true

# Logging
qap.logging.enabled=true
qap.logging.min.level=DEBUG
qap.logging.max.entries=1000
qap.logging.max.message.length=10000
qap.logging.capture.stacktraces=true
qap.logging.include.mdc=true
qap.logging.include.markers=true
qap.logging.logger.patterns=

# Stack traces
qap.stacktrace.max.lines=200
qap.stacktrace.head.lines=50
qap.stacktrace.tail.lines=20
qap.stacktrace.keep.until.framework.exit=false
```

## File Location

**Standard location:**
```
src/test/resources/qap.properties
```

**Multi-module projects:**
```
my-project/
├── module-a/
│   └── src/test/resources/qap.properties  ← Module-specific config
├── module-b/
│   └── src/test/resources/qap.properties  ← Module-specific config
└── shared/
    └── src/test/resources/qap.properties  ← Shared config
```

Each module can have its own configuration.

## Example Complete File

```properties
# ========================================
# Application & Environment
# ========================================
qap.app.name=PaymentService
qap.test.environment=TEST
qap.run.environment=CI
qap.user=jenkins

# ========================================
# Reporting
# ========================================
qap.reporting.enabled=true
qap.api.key=demo-api-key-12345

# ========================================
# Log Capture
# ========================================
qap.logging.enabled=true
qap.logging.min.level=DEBUG
qap.logging.max.entries=1000
qap.logging.max.message.length=10000
qap.logging.capture.stacktraces=true
qap.logging.include.mdc=true
qap.logging.include.markers=true

# Only capture application logs
qap.logging.logger.patterns=com.example.*

# ========================================
# Stack Trace Configuration
# ========================================
qap.stacktrace.max.lines=200
qap.stacktrace.head.lines=50
qap.stacktrace.tail.lines=20
qap.stacktrace.keep.until.framework.exit=false
```

## Troubleshooting

### Properties not loading?
1. Check file location: `src/test/resources/qap.properties`
2. Check file encoding: Should be UTF-8
3. Check syntax: `key=value` (no spaces around `=`)
4. Check logs: QAP logs warnings for missing/invalid properties

### Configuration not applying?
1. Rebuild project: `./gradlew clean build`
2. Check for typos in property names
3. Verify property values are valid (e.g., log levels)
4. Check if system properties are overriding

### Invalid values?
QAP falls back to defaults for invalid values:
```properties
qap.logging.max.entries=abc       # Invalid → uses default: 1000
qap.stacktrace.max.lines=999999   # Too large → no problem, accepted
qap.logging.min.level=INVALID     # Invalid → uses default: DEBUG
```

## Quick Reference

| Category | Key Properties | Default |
|----------|---------------|---------|
| **Reporting** | `qap.reporting.enabled` | `true` |
| **Logging** | `qap.logging.enabled`<br>`qap.logging.min.level`<br>`qap.logging.max.entries` | `true`<br>`DEBUG`<br>`1000` |
| **Stack Traces** | `qap.stacktrace.max.lines`<br>`qap.stacktrace.head.lines`<br>`qap.stacktrace.tail.lines` | `200`<br>`50`<br>`20` |

## See Also

- [Stack Trace Capping Implementation](STACK_TRACE_CAPPING_IMPLEMENTATION.md)
- [Logging Configuration Details](PROPERTY_BASED_LOGGING_CONFIG.md)
- Sample configurations in `test-app/src/test/resources/qap.properties`
