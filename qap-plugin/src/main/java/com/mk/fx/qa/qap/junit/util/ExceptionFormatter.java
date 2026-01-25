package com.mk.fx.qa.qap.junit.util;

import com.mk.fx.qa.qap.junit.model.QAPFailure;
import com.mk.fx.qa.qap.junit.model.QAPFailureLocation;
import com.mk.fx.qa.qap.junit.model.QAPRootCause;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ExceptionFormatter {

  private ExceptionFormatter() {}

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
    String stackTrace = stackTraceOf(throwable);

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
   * Extracts location information from the first stack trace element.
   *
   * @param throwable the exception to extract location from
   * @return QAPFailureLocation or null if no stack trace available
   */
  private static QAPFailureLocation extractLocation(Throwable throwable) {
    StackTraceElement[] stackTrace = throwable.getStackTrace();
    if (stackTrace == null || stackTrace.length == 0) {
      return null;
    }

    StackTraceElement firstElement = stackTrace[0];
    String className = firstElement.getClassName();
    String methodName = firstElement.getMethodName();
    String fileName = firstElement.getFileName();
    int lineNumber = firstElement.getLineNumber();

    return new QAPFailureLocation(
        className, methodName, fileName, lineNumber > 0 ? lineNumber : null);
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
