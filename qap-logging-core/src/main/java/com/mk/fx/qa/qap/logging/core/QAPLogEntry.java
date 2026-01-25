package com.mk.fx.qa.qap.logging.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Represents a single log entry captured during test execution. Immutable and thread-safe. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAPLogEntry {

  private final Instant timestamp;
  private final QAPLogLevel level;
  private final String loggerName;
  private final String threadName;
  private final String message;
  private final String throwableMessage;
  private final String[] stackTrace;
  private final Map<String, String> mdc;
  private final Set<String> markers;

  @JsonCreator
  public QAPLogEntry(
      @JsonProperty("timestamp") Instant timestamp,
      @JsonProperty("level") QAPLogLevel level,
      @JsonProperty("logger") String loggerName,
      @JsonProperty("thread") String threadName,
      @JsonProperty("message") String message,
      @JsonProperty("throwable") String throwableMessage,
      @JsonProperty("stackTrace") String[] stackTrace,
      @JsonProperty("mdc") Map<String, String> mdc,
      @JsonProperty("markers") Set<String> markers) {
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    this.level = Objects.requireNonNull(level, "level cannot be null");
    this.loggerName = Objects.requireNonNull(loggerName, "loggerName cannot be null");
    this.threadName = threadName;
    this.message = message;
    this.throwableMessage = throwableMessage;
    this.stackTrace = stackTrace;
    this.mdc = mdc != null ? Map.copyOf(mdc) : Collections.emptyMap();
    this.markers = markers != null ? Set.copyOf(markers) : Collections.emptySet();
  }

  @JsonProperty("timestamp")
  public Instant getTimestamp() {
    return timestamp;
  }

  @JsonProperty("level")
  public QAPLogLevel getLevel() {
    return level;
  }

  @JsonProperty("logger")
  public String getLoggerName() {
    return loggerName;
  }

  @JsonProperty("thread")
  public String getThreadName() {
    return threadName;
  }

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("throwable")
  public String getThrowableMessage() {
    return throwableMessage;
  }

  @JsonProperty("stackTrace")
  public String[] getStackTrace() {
    return stackTrace != null ? stackTrace.clone() : null;
  }

  @JsonProperty("mdc")
  public Map<String, String> getMdc() {
    return mdc;
  }

  @JsonProperty("markers")
  public Set<String> getMarkers() {
    return markers;
  }

  /**
   * Returns a compact string representation for text-based output. Format: "LEVEL - Logger -
   * Message"
   */
  public String toCompactString() {
    return String.format("%s - %s - %s", level, loggerName, message);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QAPLogEntry that = (QAPLogEntry) o;
    return Objects.equals(timestamp, that.timestamp)
        && level == that.level
        && Objects.equals(loggerName, that.loggerName)
        && Objects.equals(threadName, that.threadName)
        && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, level, loggerName, threadName, message);
  }

  @Override
  public String toString() {
    return "QAPLogEntry{"
        + "timestamp="
        + timestamp
        + ", level="
        + level
        + ", logger='"
        + loggerName
        + "', message='"
        + message
        + "'}";
  }

  /** Builder for creating QAPLogEntry instances. */
  public static class Builder {
    private Instant timestamp;
    private QAPLogLevel level;
    private String loggerName;
    private String threadName;
    private String message;
    private String throwableMessage;
    private String[] stackTrace;
    private Map<String, String> mdc;
    private Set<String> markers;

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder level(QAPLogLevel level) {
      this.level = level;
      return this;
    }

    public Builder loggerName(String loggerName) {
      this.loggerName = loggerName;
      return this;
    }

    public Builder threadName(String threadName) {
      this.threadName = threadName;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder throwableMessage(String throwableMessage) {
      this.throwableMessage = throwableMessage;
      return this;
    }

    public Builder stackTrace(String[] stackTrace) {
      this.stackTrace = stackTrace;
      return this;
    }

    public Builder mdc(Map<String, String> mdc) {
      this.mdc = mdc;
      return this;
    }

    public Builder markers(Set<String> markers) {
      this.markers = markers;
      return this;
    }

    public QAPLogEntry build() {
      return new QAPLogEntry(
          timestamp,
          level,
          loggerName,
          threadName,
          message,
          throwableMessage,
          stackTrace,
          mdc,
          markers);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
