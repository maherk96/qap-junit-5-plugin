# QAP Logging Core Module

Framework-agnostic logging abstraction for capturing test logs in the QAP JUnit 5 Plugin.

## Overview

This module provides the core interfaces and models for log capture, allowing framework-specific implementations (Logback, Log4j2, etc.) to be discovered and used at runtime without hardcoded dependencies.

## Key Components

### 1. QAPLogLevel
Enum representing log severity levels aligned with common logging frameworks.

```java
public enum QAPLogLevel {
  TRACE, DEBUG, INFO, WARN, ERROR, FATAL
}
```

### 2. QAPLogEntry
Immutable record representing a single captured log entry.

```java
QAPLogEntry entry = QAPLogEntry.builder()
    .timestamp(Instant.now())
    .level(QAPLogLevel.INFO)
    .loggerName("com.example.MyClass")
    .threadName("main")
    .message("Application started")
    .mdc(Map.of("requestId", "123"))
    .markers(Set.of("IMPORTANT"))
    .build();
```

### 3. QAPLogCapturer (Interface)
Framework-specific interface that implementations must provide.

Key methods:
- `startCapture(testId, config)` - Begin capturing logs for a test
- `stopCapture(testId)` - Stop and return captured logs
- `getFrameworkName()` - Returns "Logback", "Log4j2", etc.
- `isAvailable()` - Check if framework classes exist on classpath

### 4. QAPLogCapturerFactory (Interface)
Factory interface for ServiceLoader discovery.

```java
public interface QAPLogCapturerFactory {
  QAPLogCapturer create();
  String getName();
}
```

### 5. QAPLogCapturerRegistry
Central registry that discovers and manages log capturers using Java ServiceLoader.

```java
QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
registry.discover();

Optional<QAPLogCapturer> capturer = registry.getAvailableCapturer();
if (capturer.isPresent()) {
    System.out.println("Using: " + capturer.get().getFrameworkName());
}
```

### 6. QAPLogCaptureConfig
Configuration for log capture behavior.

```java
QAPLogCaptureConfig config = QAPLogCaptureConfig.builder()
    .enabled(true)
    .minLevel(QAPLogLevel.INFO)
    .maxEntriesPerTest(1000)
    .maxMessageLength(10_000)
    .captureStackTraces(true)
    .includeMdc(true)
    .includeMarkers(true)
    .addLoggerPattern("com.example.*")
    .addLoggerPattern("org.springframework.*")
    .build();
```

## ServiceLoader Discovery

Framework implementations are discovered at runtime using Java's ServiceLoader mechanism.

### How It Works

1. **Implementation Module** (e.g., qap-logging-logback):
   ```
   src/main/resources/
   └── META-INF/
       └── services/
           └── com.mk.fx.qa.qap.logging.core.QAPLogCapturerFactory
               Content: com.mk.fx.qa.qap.logging.logback.LogbackCapturerFactory
   ```

2. **Runtime Discovery**:
   ```java
   ServiceLoader<QAPLogCapturerFactory> loader = 
       ServiceLoader.load(QAPLogCapturerFactory.class);
   
   for (QAPLogCapturerFactory factory : loader) {
       QAPLogCapturer capturer = factory.create();
       if (capturer.isAvailable()) {
           // Use this capturer
       }
   }
   ```

## Thread Safety

All components are designed for thread-safe operation:
- **QAPLogEntry**: Immutable
- **QAPLogCaptureConfig**: Immutable
- **QAPLogCapturerRegistry**: Synchronized discovery, thread-safe operations
- **QAPLogCapturer**: Implementations must be thread-safe (ThreadLocal recommended)

## Memory Management

Configuration includes safeguards against memory issues:
- `maxEntriesPerTest` (default: 1000) - Prevents unbounded growth
- `maxMessageLength` (default: 10,000) - Truncates large messages
- Bounded ring buffers in implementations
- Cleanup after test completion

## Usage Pattern

### In Framework Implementation (e.g., Logback)

```java
public class LogbackCapturer implements QAPLogCapturer {
    private final ThreadLocal<List<QAPLogEntry>> buffer = new ThreadLocal<>();
    
    @Override
    public void startCapture(String testId, QAPLogCaptureConfig config) {
        buffer.set(new ArrayList<>());
        // Attach appender to Logback logger context
    }
    
    @Override
    public List<QAPLogEntry> stopCapture(String testId) {
        List<QAPLogEntry> logs = buffer.get();
        buffer.remove(); // Clean up!
        return logs;
    }
    
    @Override
    public String getFrameworkName() {
        return "Logback";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Class.forName("ch.qos.logback.classic.LoggerContext");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
```

### In JUnit Extension

```java
public class QAPJunitExtension implements BeforeEachCallback, AfterEachCallback {
    private final QAPLogCapturerRegistry registry = new QAPLogCapturerRegistry();
    private QAPLogCapturer capturer;
    
    @Override
    public void beforeAll(ExtensionContext context) {
        registry.discover();
        capturer = registry.getAvailableCapturer().orElse(null);
        if (capturer != null) {
            log.info("Log capture enabled: {}", capturer.getFrameworkName());
        }
    }
    
    @Override
    public void beforeEach(ExtensionContext context) {
        if (capturer != null) {
            String testId = context.getUniqueId();
            QAPLogCaptureConfig config = QAPLogCaptureConfig.defaultConfig();
            capturer.startCapture(testId, config);
        }
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        if (capturer != null) {
            String testId = context.getUniqueId();
            List<QAPLogEntry> logs = capturer.stopCapture(testId);
            // Attach logs to test case...
        }
    }
}
```

## Testing

The module includes comprehensive unit tests:

```bash
./gradlew :qap-logging-core:test
```

Test coverage:
- ✅ QAPLogLevelTest - Level ordering and comparison
- ✅ QAPLogEntryTest - Builder, immutability, serialization
- ✅ QAPLogCaptureConfigTest - Configuration validation, pattern matching

Total: 23 tests, all passing

## Dependencies

### Runtime
- `org.slf4j:slf4j-api:2.0.13` - Internal logging only
- `com.fasterxml.jackson.core:jackson-databind:2.17.1` - JSON serialization

### Compile-Only
- None (framework-agnostic)

### Test
- JUnit 5
- Mockito

## Next Steps

After implementing this core module, the next phases are:

1. **Phase 2**: qap-logging-logback module (Logback implementation)
2. **Phase 3**: qap-logging-log4j2 module (Log4j2 implementation)
3. **Phase 4**: Integration with QAPJunitExtension
4. **Phase 5**: Documentation and examples

## Design Principles

1. **Zero Dependencies**: Core module has no logging framework dependencies
2. **Discovery over Configuration**: ServiceLoader auto-detects implementations
3. **Fail Gracefully**: Missing implementations don't break tests
4. **Thread Safe**: Safe for parallel test execution
5. **Memory Efficient**: Bounded buffers prevent OOM
6. **Extensible**: Easy to add support for new frameworks

---

*Version: 1.1.0-SNAPSHOT*  
*Module: qap-logging-core*  
*Status: ✅ Phase 1 Complete*
