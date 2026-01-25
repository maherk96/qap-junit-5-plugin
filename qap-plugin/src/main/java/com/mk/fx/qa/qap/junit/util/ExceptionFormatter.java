package com.mk.fx.qa.qap.junit.util;

import com.mk.fx.qa.qap.junit.model.QAPFailure;
import com.mk.fx.qa.qap.junit.model.QAPFailureLocation;
import com.mk.fx.qa.qap.junit.model.QAPRootCause;
import com.mk.fx.qa.qap.junit.model.StackTraceConfig;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ExceptionFormatter {

  private static StackTraceConfig stackTraceConfig = StackTraceConfig.defaultConfig();

  private ExceptionFormatter() {}

  /**
   * Sets the stack trace configuration for capping and formatting.
   *
   * @param config the stack trace configuration
   */
  public static void setStackTraceConfig(StackTraceConfig config) {
    if (config != null) {
      stackTraceConfig = config;
    }
  }

  public static byte[] toBytes(String text) {
    if (text == null) {
      return new byte[0];
    }
    return text.getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] toBytes(Throwable throwable) {
    if (throwable == null) {
      return new byte[0];
    }
    return toBytes(stackTraceOf(throwable));
  }

  public static String stackTraceOf(Throwable throwable) {
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  /**
   * Converts a Throwable to a structured QAPFailure object.
   *
   * @param throwable the exception/error to convert
   * @return QAPFailure object, or null if throwable is null
   */
  public static QAPFailure toFailure(Throwable throwable) {
    if (throwable == null) {
      return null;
    }
    return toFailure(throwable, new ArrayList<>());
  }

  /**
   * Converts a Throwable to a structured QAPFailure object, handling circular references.
   *
   * @param throwable the exception/error to convert
   * @param seen list of already processed exceptions to prevent infinite recursion
   * @return QAPFailure object
   */
  private static QAPFailure toFailure(Throwable throwable, List<Throwable> seen) {
    if (throwable == null) {
      return null;
    }

    // Prevent infinite recursion with circular references
    if (seen.contains(throwable)) {
      return new QAPFailure(
          throwable.getClass().getName(),
          "[Circular reference detected]",
          null,
          null,
          null,
          null,
          null);
    }
    seen.add(throwable);

    String type = throwable.getClass().getName();
    String message = throwable.getMessage();
    String stackTrace = capStackTrace(throwable); // ✅ Use capped stack trace

    // Extract location from stack trace
    QAPFailureLocation location = extractLocation(throwable);

    // Extract root cause from exception cause chain
    QAPRootCause rootCause = extractRootCause(throwable);

    // Handle cause
    QAPFailure causedBy = null;
    Throwable cause = throwable.getCause();
    if (cause != null && cause != throwable) {
      // Pass the same seen list to maintain circular reference detection
      causedBy = toFailure(cause, seen);
    }

    // Handle suppressed exceptions
    List<QAPFailure> suppressed = new ArrayList<>();
    Throwable[] suppressedExceptions = throwable.getSuppressed();
    if (suppressedExceptions != null && suppressedExceptions.length > 0) {
      for (Throwable suppressedException : suppressedExceptions) {
        // Pass the same seen list to maintain circular reference detection
        QAPFailure suppressedFailure = toFailure(suppressedException, seen);
        if (suppressedFailure != null) {
          suppressed.add(suppressedFailure);
        }
      }
    }

    return new QAPFailure(type, message, stackTrace, causedBy, suppressed, location, rootCause);
  }

  /**
   * Caps the stack trace based on configuration settings.
   *
   * @param throwable the exception to extract stack trace from
   * @return capped stack trace string
   */
  private static String capStackTrace(Throwable throwable) {
    String fullStackTrace = stackTraceOf(throwable);
    if (fullStackTrace == null || fullStackTrace.isEmpty()) {
      return fullStackTrace;
    }

    String[] lines = fullStackTrace.split("\r?\n");
    int totalLines = lines.length;

    // If unlimited or within limits, return as-is
    if (stackTraceConfig.getMaxLines() < 0 || totalLines <= stackTraceConfig.getMaxLines()) {
      return fullStackTrace;
    }

    // Apply capping strategy
    List<String> cappedLines = new ArrayList<>();

    if (stackTraceConfig.isKeepUntilFrameworkExit()) {
      // Strategy: Keep lines until we exit user-code frames
      cappedLines.addAll(capUntilFrameworkExit(lines));
    } else {
      // Strategy: Keep first N + last M lines
      int headLines = Math.min(stackTraceConfig.getHeadLines(), totalLines);
      int tailLines = Math.min(stackTraceConfig.getTailLines(), totalLines - headLines);

      // Add head lines
      for (int i = 0; i < headLines; i++) {
        cappedLines.add(lines[i]);
      }

      // Add separator if we're truncating
      if (headLines + tailLines < totalLines) {
        int omittedLines = totalLines - headLines - tailLines;
        cappedLines.add(String.format("\t... %d more lines omitted ...", omittedLines));
      }

      // Add tail lines
      int tailStartIndex = totalLines - tailLines;
      for (int i = tailStartIndex; i < totalLines; i++) {
        cappedLines.add(lines[i]);
      }
    }

    return String.join("\n", cappedLines);
  }

  /**
   * Caps stack trace by keeping lines until we exit user-code frames.
   *
   * @param lines all stack trace lines
   * @return list of lines to keep
   */
  private static List<String> capUntilFrameworkExit(String[] lines) {
    List<String> result = new ArrayList<>();
    boolean inUserCode = false;

    for (int i = 0; i < lines.length && result.size() < stackTraceConfig.getMaxLines(); i++) {
      String line = lines[i];
      result.add(line);

      // Check if this is a user-code frame
      boolean isUserFrame = !isFrameworkLine(line);

      if (isUserFrame) {
        inUserCode = true;
      } else if (inUserCode) {
        // We were in user code and now hit framework code - stop here
        if (i < lines.length - 1) {
          int omittedLines = lines.length - i - 1;
          result.add(String.format("\t... %d more lines omitted ...", omittedLines));
        }
        break;
      }
    }

    return result;
  }

  /**
   * Checks if a stack trace line represents framework code (not user code).
   *
   * @param line stack trace line
   * @return true if this is a framework line
   */
  private static boolean isFrameworkLine(String line) {
    // Skip the first line (exception type and message)
    if (!line.trim().startsWith("at ")) {
      return false;
    }

    // Extract class name from "at com.example.Class.method(File.java:123)"
    String trimmed = line.trim();
    if (trimmed.startsWith("at ")) {
      String rest = trimmed.substring(3);
      int parenIndex = rest.indexOf('(');
      if (parenIndex > 0) {
        String classAndMethod = rest.substring(0, parenIndex);
        int lastDotIndex = classAndMethod.lastIndexOf('.');
        if (lastDotIndex > 0) {
          String className = classAndMethod.substring(0, lastDotIndex);
          return isFrameworkClass(className);
        }
      }
    }

    return false;
  }

  /**
   * Extracts location information from the first user-code stack trace element. Skips JUnit and
   * internal framework frames to find the actual test method that failed.
   *
   * @param throwable the exception to extract location from
   * @return QAPFailureLocation or null if no stack trace available
   */
  private static QAPFailureLocation extractLocation(Throwable throwable) {
    StackTraceElement[] stackTrace = throwable.getStackTrace();
    if (stackTrace == null || stackTrace.length == 0) {
      return null;
    }

    // Find the first user-code frame (skip JUnit, java.base, org.gradle, etc.)
    StackTraceElement userFrame = null;
    for (StackTraceElement element : stackTrace) {
      String className = element.getClassName();
      // Skip framework and infrastructure classes
      if (!isFrameworkClass(className)) {
        userFrame = element;
        break;
      }
    }

    // Fall back to first element if no user frame found
    if (userFrame == null) {
      userFrame = stackTrace[0];
    }

    String className = userFrame.getClassName();
    String methodName = userFrame.getMethodName();
    String fileName = userFrame.getFileName();
    int lineNumber = userFrame.getLineNumber();

    return new QAPFailureLocation(
        className, methodName, fileName, lineNumber > 0 ? lineNumber : null);
  }

  /**
   * Determines if a class name belongs to framework/infrastructure code that should be skipped when
   * finding the user-code location.
   *
   * @param className fully qualified class name
   * @return true if this is a framework class to skip
   */
  private static boolean isFrameworkClass(String className) {
    return className.startsWith("org.junit.")
        || className.startsWith("org.opentest4j.")
        || className.startsWith("java.base/")
        || className.startsWith("jdk.internal.")
        || className.startsWith("java.lang.reflect.")
        || className.startsWith("org.gradle.")
        || className.startsWith("worker.org.gradle.")
        || className.startsWith("jdk.proxy")
        || className.startsWith("com.mk.fx.qa.qap.junit.extension."); // Skip our own extension code
  }

  /**
   * Extracts root cause from the exception cause chain by walking getCause() until the last
   * non-null cause is found. This is more reliable than parsing stack traces because frameworks
   * often wrap exceptions.
   *
   * @param throwable the exception to extract root cause from
   * @return QAPRootCause with type and message, or null if no cause chain exists
   */
  private static QAPRootCause extractRootCause(Throwable throwable) {
    if (throwable == null) {
      return null;
    }

    // Walk the cause chain to find the root cause
    Throwable root = throwable;
    Throwable cause = throwable.getCause();

    while (cause != null && cause != root) {
      root = cause;
      cause = cause.getCause();
    }

    // If we found a different root cause, return it
    if (root != throwable) {
      String rootType = root.getClass().getName();
      String rootMessage = root.getMessage();
      return new QAPRootCause(rootType, rootMessage);
    }

    // If no cause chain, return null (root cause is the exception itself)
    return null;
  }

  /**
   * Creates a QAPFailure from a simple message string (for disabled tests, etc.).
   *
   * @param message the message to create a failure from
   * @return QAPFailure object
   */
  public static QAPFailure fromMessage(String message) {
    if (message == null) {
      return null;
    }
    return new QAPFailure("java.lang.RuntimeException", message, null, null, null, null, null);
  }
}
