package com.mk.fx.qa.qap.logging.core;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for discovering and managing QAPLogCapturer implementations. Uses Java ServiceLoader to
 * find implementations at runtime.
 *
 * <p>Thread-safe and can be used as a singleton.
 */
public class QAPLogCapturerRegistry {

  private static final Logger log = LoggerFactory.getLogger(QAPLogCapturerRegistry.class);

  private final List<QAPLogCapturer> availableCapturers = new ArrayList<>();
  private final Map<String, QAPLogCapturer> capturersByName = new HashMap<>();
  private boolean discovered = false;

  /**
   * Discovers all available log capturer implementations using ServiceLoader. This method is
   * idempotent - calling it multiple times has no additional effect.
   *
   * @return this registry for method chaining
   */
  public synchronized QAPLogCapturerRegistry discover() {
    if (discovered) {
      log.debug("Discovery already completed, skipping");
      return this;
    }

    log.debug("Starting discovery of QAPLogCapturer implementations...");

    try {
      ServiceLoader<QAPLogCapturerFactory> loader = ServiceLoader.load(QAPLogCapturerFactory.class);

      int found = 0;
      int available = 0;

      for (QAPLogCapturerFactory factory : loader) {
        found++;
        try {
          QAPLogCapturer capturer = factory.create();
          String frameworkName = capturer.getFrameworkName();

          log.debug(
              "Found capturer factory: {} (framework: {})",
              factory.getClass().getName(),
              frameworkName);

          if (capturer.isAvailable()) {
            available++;
            availableCapturers.add(capturer);
            capturersByName.put(frameworkName.toLowerCase(), capturer);
            log.info(
                "✅ Registered log capturer: {} (priority: {})",
                frameworkName,
                capturer.getPriority());
          } else {
            log.debug(
                "⚠️ Capturer '{}' found but framework classes not available on classpath",
                frameworkName);
          }
        } catch (Exception e) {
          log.warn(
              "Failed to create capturer from factory {}: {}",
              factory.getClass().getName(),
              e.getMessage());
        }
      }

      // Sort by priority (highest first)
      availableCapturers.sort(Comparator.comparingInt(QAPLogCapturer::getPriority).reversed());

      discovered = true;

      if (available == 0) {
        log.info("No log capturers available - log capture will be disabled");
      } else {
        log.info(
            "Discovery complete: {} capturer(s) found, {} available: {}",
            found,
            available,
            availableCapturers.stream()
                .map(QAPLogCapturer::getFrameworkName)
                .collect(Collectors.joining(", ")));
      }

    } catch (ServiceConfigurationError e) {
      log.error("ServiceLoader configuration error during capturer discovery", e);
    }

    return this;
  }

  /**
   * Returns the first available capturer (highest priority). Automatically calls discover() if not
   * already done.
   *
   * @return Optional containing the capturer, or empty if none available
   */
  public Optional<QAPLogCapturer> getAvailableCapturer() {
    if (!discovered) {
      discover();
    }
    return availableCapturers.isEmpty() ? Optional.empty() : Optional.of(availableCapturers.get(0));
  }

  /**
   * Returns a capturer by framework name (case-insensitive). Automatically calls discover() if not
   * already done.
   *
   * @param frameworkName the framework name (e.g., "logback", "log4j2")
   * @return Optional containing the capturer, or empty if not found
   */
  public Optional<QAPLogCapturer> getCapturerByName(String frameworkName) {
    if (!discovered) {
      discover();
    }
    Objects.requireNonNull(frameworkName, "frameworkName cannot be null");
    return Optional.ofNullable(capturersByName.get(frameworkName.toLowerCase()));
  }

  /**
   * Returns all available capturers sorted by priority (highest first). Automatically calls
   * discover() if not already done.
   *
   * @return unmodifiable list of available capturers
   */
  public List<QAPLogCapturer> getAllAvailableCapturers() {
    if (!discovered) {
      discover();
    }
    return Collections.unmodifiableList(availableCapturers);
  }

  /**
   * Checks if any capturers are available. Automatically calls discover() if not already done.
   *
   * @return true if at least one capturer is available
   */
  public boolean hasAvailableCapturer() {
    if (!discovered) {
      discover();
    }
    return !availableCapturers.isEmpty();
  }

  /**
   * Returns the number of available capturers.
   *
   * @return number of available capturers
   */
  public int getAvailableCount() {
    if (!discovered) {
      discover();
    }
    return availableCapturers.size();
  }

  /**
   * Shuts down all registered capturers, releasing resources. Should be called when the registry is
   * no longer needed.
   */
  public synchronized void shutdown() {
    log.debug("Shutting down {} capturer(s)", availableCapturers.size());
    for (QAPLogCapturer capturer : availableCapturers) {
      try {
        capturer.shutdown();
      } catch (Exception e) {
        log.warn(
            "Error shutting down capturer {}: {}", capturer.getFrameworkName(), e.getMessage());
      }
    }
    availableCapturers.clear();
    capturersByName.clear();
    discovered = false;
  }

  /** Resets the registry to allow re-discovery. Useful for testing or dynamic classpath changes. */
  public synchronized void reset() {
    shutdown();
  }

  @Override
  public String toString() {
    return "QAPLogCapturerRegistry{"
        + "discovered="
        + discovered
        + ", available="
        + availableCapturers.size()
        + ", frameworks="
        + availableCapturers.stream()
            .map(QAPLogCapturer::getFrameworkName)
            .collect(Collectors.joining(", "))
        + '}';
  }
}
