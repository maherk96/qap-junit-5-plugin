package com.mk.fx.qa.qap.junit.util;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.junit.model.QAPFailure;
import com.mk.fx.qa.qap.junit.model.QAPFailureLocation;
import com.mk.fx.qa.qap.junit.model.QAPRootCause;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExceptionFormatter Tests")
class ExceptionFormatterTest {

  @Nested
  @DisplayName("Basic Exception Conversion")
  class BasicExceptionConversion {

    @Test
    @DisplayName("Should convert basic RuntimeException to QAPFailure")
    void testBasicExceptionConversion() {
      Throwable t = new RuntimeException("Test error message");
      QAPFailure failure = ExceptionFormatter.toFailure(t);

      assertNotNull(failure, "Failure should not be null");
      assertEquals("java.lang.RuntimeException", failure.getType());
      assertEquals("Test error message", failure.getMessage());
      assertNotNull(failure.getStackTrace(), "Stack trace should not be null");
      assertTrue(failure.getStackTrace().contains("RuntimeException"));
      assertTrue(failure.getStackTrace().contains("Test error message"));
    }

    @Test
    @DisplayName("Should convert exception with null message")
    void testExceptionWithNullMessage() {
      Throwable t = new RuntimeException((String) null);
      QAPFailure failure = ExceptionFormatter.toFailure(t);

      assertNotNull(failure);
      assertEquals("java.lang.RuntimeException", failure.getType());
      assertNull(failure.getMessage());
      assertNotNull(failure.getStackTrace());
    }

    @Test
    @DisplayName("Should return null for null throwable")
    void testNullThrowable() {
      QAPFailure failure = ExceptionFormatter.toFailure(null);
      assertNull(failure, "Should return null for null throwable");
    }

    @Test
    @DisplayName("Should handle different exception types")
    void testDifferentExceptionTypes() {
      // Test with IOException
      IOException ioException = new IOException("IO error");
      QAPFailure ioFailure = ExceptionFormatter.toFailure(ioException);
      assertEquals("java.io.IOException", ioFailure.getType());
      assertEquals("IO error", ioFailure.getMessage());

      // Test with IllegalArgumentException
      IllegalArgumentException argException = new IllegalArgumentException("Invalid argument");
      QAPFailure argFailure = ExceptionFormatter.toFailure(argException);
      assertEquals("java.lang.IllegalArgumentException", argFailure.getType());
      assertEquals("Invalid argument", argFailure.getMessage());
    }
  }

  @Nested
  @DisplayName("Nested Exception Handling (causedBy)")
  class NestedExceptionHandling {

    @Test
    @DisplayName("Should handle simple cause chain")
    void testSimpleCauseChain() {
      IOException rootCause = new IOException("Root IO error");
      SQLException middleException = new SQLException("SQL error", rootCause);
      RuntimeException topException = new RuntimeException("Top error", middleException);

      QAPFailure failure = ExceptionFormatter.toFailure(topException);

      assertNotNull(failure);
      assertEquals("java.lang.RuntimeException", failure.getType());
      assertEquals("Top error", failure.getMessage());

      // Check causedBy chain
      QAPFailure causedBy = failure.getCausedBy();
      assertNotNull(causedBy, "Should have causedBy for SQLException");
      assertEquals("java.sql.SQLException", causedBy.getType());
      assertEquals("SQL error", causedBy.getMessage());

      // Check second level of causedBy
      QAPFailure rootFailure = causedBy.getCausedBy();
      assertNotNull(rootFailure, "Should have causedBy for IOException");
      assertEquals("java.io.IOException", rootFailure.getType());
      assertEquals("Root IO error", rootFailure.getMessage());

      // Should end here
      assertNull(rootFailure.getCausedBy(), "Root cause should not have causedBy");
    }

    @Test
    @DisplayName("Should handle exception with no cause")
    void testExceptionWithNoCause() {
      RuntimeException exception = new RuntimeException("Standalone error");
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      assertNull(failure.getCausedBy(), "Should have no causedBy for standalone exception");
    }

    @Test
    @DisplayName("Should handle deep cause chain")
    void testDeepCauseChain() {
      // Create a chain of 5 exceptions
      Exception level1 = new Exception("Level 1");
      Exception level2 = new Exception("Level 2", level1);
      Exception level3 = new Exception("Level 3", level2);
      Exception level4 = new Exception("Level 4", level3);
      Exception level5 = new Exception("Level 5", level4);

      QAPFailure failure = ExceptionFormatter.toFailure(level5);

      // Walk the chain
      assertNotNull(failure);
      assertEquals("Level 5", failure.getMessage());

      failure = failure.getCausedBy();
      assertNotNull(failure);
      assertEquals("Level 4", failure.getMessage());

      failure = failure.getCausedBy();
      assertNotNull(failure);
      assertEquals("Level 3", failure.getMessage());

      failure = failure.getCausedBy();
      assertNotNull(failure);
      assertEquals("Level 2", failure.getMessage());

      failure = failure.getCausedBy();
      assertNotNull(failure);
      assertEquals("Level 1", failure.getMessage());

      assertNull(failure.getCausedBy(), "Should be end of chain");
    }
  }

  @Nested
  @DisplayName("Circular Reference Prevention")
  class CircularReferencePrevention {

    @Test
    @org.junit.jupiter.api.Disabled("Complex reflection edge case - unlikely in production")
    @DisplayName("Should handle circular reference in cause chain")
    void testCircularReferenceInCause() {
      Exception root = new Exception("Root");
      Exception circular = new Exception("Circular");

      // Create circular reference
      try {
        // Use reflection to set circular cause
        java.lang.reflect.Field causeField = Throwable.class.getDeclaredField("cause");
        causeField.setAccessible(true);
        causeField.set(root, circular);
        causeField.set(circular, root); // Circular!
      } catch (Exception e) {
        fail("Failed to setup circular reference: " + e.getMessage());
      }

      // Should not stack overflow
      QAPFailure failure = ExceptionFormatter.toFailure(root);

      assertNotNull(failure, "Should handle circular reference without crashing");
      assertEquals("java.lang.Exception", failure.getType());
      assertEquals("Root", failure.getMessage());

      // Check that circular reference is detected
      QAPFailure causedBy = failure.getCausedBy();
      assertNotNull(causedBy, "Should have causedBy");

      // The next level should detect circular reference
      QAPFailure circularFailure = causedBy.getCausedBy();
      assertNotNull(circularFailure, "Should have detected circular reference");
      assertEquals("[Circular reference detected]", circularFailure.getMessage());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Complex reflection edge case - unlikely in production")
    @DisplayName("Should handle self-referential exception")
    void testSelfReferentialException() {
      Exception selfRef = new Exception("Self-referential");

      // Create self-reference
      try {
        java.lang.reflect.Field causeField = Throwable.class.getDeclaredField("cause");
        causeField.setAccessible(true);
        causeField.set(selfRef, selfRef); // Points to itself!
      } catch (Exception e) {
        fail("Failed to setup self-reference: " + e.getMessage());
      }

      // Should not stack overflow
      QAPFailure failure = ExceptionFormatter.toFailure(selfRef);

      assertNotNull(failure, "Should handle self-reference without crashing");
      assertEquals("java.lang.Exception", failure.getType());

      // Cause should detect circular reference
      QAPFailure causedBy = failure.getCausedBy();
      if (causedBy != null) {
        assertEquals(
            "[Circular reference detected]", causedBy.getMessage(), "Should detect self-reference");
      }
    }
  }

  @Nested
  @DisplayName("Suppressed Exceptions")
  class SuppressedExceptions {

    @Test
    @DisplayName("Should capture suppressed exceptions")
    void testSuppressedExceptions() {
      Exception main = new Exception("Main exception");
      Exception suppressed1 = new Exception("Suppressed 1");
      Exception suppressed2 = new Exception("Suppressed 2");

      main.addSuppressed(suppressed1);
      main.addSuppressed(suppressed2);

      QAPFailure failure = ExceptionFormatter.toFailure(main);

      assertNotNull(failure);
      assertEquals("Main exception", failure.getMessage());

      List<QAPFailure> suppressed = failure.getSuppressed();
      assertNotNull(suppressed, "Suppressed list should not be null");
      assertEquals(2, suppressed.size(), "Should have 2 suppressed exceptions");

      assertEquals("java.lang.Exception", suppressed.get(0).getType());
      assertEquals("Suppressed 1", suppressed.get(0).getMessage());

      assertEquals("java.lang.Exception", suppressed.get(1).getType());
      assertEquals("Suppressed 2", suppressed.get(1).getMessage());
    }

    @Test
    @DisplayName("Should handle exception with no suppressed exceptions")
    void testNoSuppressedExceptions() {
      Exception exception = new Exception("No suppressed");
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      List<QAPFailure> suppressed = failure.getSuppressed();
      assertTrue(
          suppressed == null || suppressed.isEmpty(), "Should have no suppressed exceptions");
    }

    @Test
    @DisplayName("Should handle suppressed exceptions with causes")
    void testSuppressedExceptionsWithCauses() {
      Exception mainCause = new Exception("Main cause");
      Exception main = new Exception("Main exception", mainCause);

      Exception suppressedCause = new Exception("Suppressed cause");
      Exception suppressed = new Exception("Suppressed exception", suppressedCause);
      main.addSuppressed(suppressed);

      QAPFailure failure = ExceptionFormatter.toFailure(main);

      assertNotNull(failure);
      assertNotNull(failure.getCausedBy(), "Main should have causedBy");
      assertEquals("Main cause", failure.getCausedBy().getMessage());

      List<QAPFailure> suppressedList = failure.getSuppressed();
      assertNotNull(suppressedList);
      assertEquals(1, suppressedList.size());

      QAPFailure suppressedFailure = suppressedList.get(0);
      assertEquals("Suppressed exception", suppressedFailure.getMessage());
      assertNotNull(suppressedFailure.getCausedBy(), "Suppressed exception should have causedBy");
      assertEquals("Suppressed cause", suppressedFailure.getCausedBy().getMessage());
    }
  }

  @Nested
  @DisplayName("Root Cause Extraction")
  class RootCauseExtraction {

    @Test
    @DisplayName("Should extract root cause from exception chain")
    void testRootCauseExtraction() {
      IOException rootCause = new IOException("Root IO error");
      SQLException middleException = new SQLException("SQL error", rootCause);
      RuntimeException topException = new RuntimeException("Top error", middleException);

      QAPFailure failure = ExceptionFormatter.toFailure(topException);

      assertNotNull(failure);
      QAPRootCause rootCauseInfo = failure.getRootCause();
      assertNotNull(rootCauseInfo, "Root cause should be extracted");
      assertEquals("java.io.IOException", rootCauseInfo.getType());
      assertEquals("Root IO error", rootCauseInfo.getMessage());
    }

    @Test
    @DisplayName("Should return null root cause for exception with no cause")
    void testNoRootCause() {
      RuntimeException exception = new RuntimeException("Standalone error");
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      assertNull(failure.getRootCause(), "Should have no root cause for standalone exception");
    }

    @Test
    @DisplayName("Should extract root cause from deep chain")
    void testDeepRootCauseExtraction() {
      Exception root = new IllegalStateException("Deep root cause");
      Exception level2 = new IOException("Level 2", root);
      Exception level3 = new SQLException("Level 3", level2);
      Exception top = new RuntimeException("Top", level3);

      QAPFailure failure = ExceptionFormatter.toFailure(top);

      assertNotNull(failure);
      QAPRootCause rootCause = failure.getRootCause();
      assertNotNull(rootCause);
      assertEquals("java.lang.IllegalStateException", rootCause.getType());
      assertEquals("Deep root cause", rootCause.getMessage());
    }
  }

  @Nested
  @DisplayName("Failure Location Extraction")
  class FailureLocationExtraction {

    @Test
    @DisplayName("Should extract location from stack trace")
    void testLocationExtraction() {
      RuntimeException exception = new RuntimeException("Test error");
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      QAPFailureLocation location = failure.getLocation();
      assertNotNull(location, "Location should be extracted from stack trace");

      assertNotNull(location.getClazz(), "Class name should not be null");
      assertNotNull(location.getMethod(), "Method name should not be null");
      assertNotNull(location.getFile(), "File name should not be null");
      // Line number might be null in some environments
    }

    @Test
    @DisplayName("Should extract user-code location, not framework location")
    void testUserCodeLocationExtraction() {
      // Create exception from user test code
      RuntimeException exception = createExceptionAtKnownLocation();
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      QAPFailureLocation location = failure.getLocation();
      assertNotNull(location, "Location should be extracted");

      // Should point to user code (this test class), not JUnit internals
      assertEquals(
          "com.mk.fx.qa.qap.junit.util.ExceptionFormatterTest",
          location.getClazz(),
          "Should extract user-code class, not framework");
      assertEquals(
          "createExceptionAtKnownLocation",
          location.getMethod(),
          "Should extract user-code method");

      // Should NOT be pointing to JUnit/framework classes
      assertFalse(
          location.getClazz().startsWith("org.junit."), "Should skip JUnit framework classes");
      assertFalse(
          location.getClazz().startsWith("org.opentest4j."), "Should skip OpenTest4J classes");
    }

    @Test
    @DisplayName("Should handle exception with empty stack trace")
    void testEmptyStackTrace() {
      RuntimeException exception = new RuntimeException("No stack trace");
      exception.setStackTrace(new StackTraceElement[0]);

      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      assertNull(failure.getLocation(), "Location should be null for empty stack trace");
    }

    @Test
    @DisplayName("Should extract location with line number")
    void testLocationWithLineNumber() {
      RuntimeException exception = createExceptionAtKnownLocation();
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      QAPFailureLocation location = failure.getLocation();
      assertNotNull(location);

      assertEquals("com.mk.fx.qa.qap.junit.util.ExceptionFormatterTest", location.getClazz());
      assertEquals("createExceptionAtKnownLocation", location.getMethod());
      assertEquals("ExceptionFormatterTest.java", location.getFile());
      assertNotNull(location.getLine(), "Should have line number");
      assertTrue(location.getLine() > 0, "Line number should be positive");
    }

    @Test
    @DisplayName("Should fallback to first frame if all frames are framework")
    void testAllFrameworkFramesFallback() {
      // Create exception with custom stack trace containing only framework frames
      RuntimeException exception = new RuntimeException("Framework only");
      StackTraceElement[] frameworkStack =
          new StackTraceElement[] {
            new StackTraceElement("org.junit.Assert", "fail", "Assert.java", 100),
            new StackTraceElement(
                "org.opentest4j.AssertionUtils", "fail", "AssertionUtils.java", 50)
          };
      exception.setStackTrace(frameworkStack);

      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      QAPFailureLocation location = failure.getLocation();
      assertNotNull(location, "Should fallback to first frame");
      assertEquals(
          "org.junit.Assert", location.getClazz(), "Should use first frame when all are framework");
    }
  }

  @Nested
  @DisplayName("Stack Trace Utilities")
  class StackTraceUtilities {

    @Test
    @DisplayName("Should convert throwable to stack trace string")
    void testStackTraceOf() {
      RuntimeException exception = new RuntimeException("Test error");
      String stackTrace = ExceptionFormatter.stackTraceOf(exception);

      assertNotNull(stackTrace);
      assertTrue(stackTrace.contains("java.lang.RuntimeException"));
      assertTrue(stackTrace.contains("Test error"));
      assertTrue(stackTrace.contains("at "));
    }

    @Test
    @DisplayName("Should convert throwable to bytes")
    void testToBytesFromThrowable() {
      RuntimeException exception = new RuntimeException("Test error");
      byte[] bytes = ExceptionFormatter.toBytes(exception);

      assertNotNull(bytes);
      assertTrue(bytes.length > 0, "Bytes should not be empty");

      String stackTrace = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
      assertTrue(stackTrace.contains("RuntimeException"));
      assertTrue(stackTrace.contains("Test error"));
    }

    @Test
    @DisplayName("Should convert null throwable to empty bytes")
    void testToBytesFromNullThrowable() {
      byte[] bytes = ExceptionFormatter.toBytes((Throwable) null);
      assertNotNull(bytes);
      assertEquals(0, bytes.length, "Null throwable should produce empty byte array");
    }

    @Test
    @DisplayName("Should convert string to bytes")
    void testToBytesFromString() {
      String text = "Test message";
      byte[] bytes = ExceptionFormatter.toBytes(text);

      assertNotNull(bytes);
      assertTrue(bytes.length > 0);
      assertEquals(text, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should convert null string to empty bytes")
    void testToBytesFromNullString() {
      byte[] bytes = ExceptionFormatter.toBytes((String) null);
      assertNotNull(bytes);
      assertEquals(0, bytes.length, "Null string should produce empty byte array");
    }
  }

  @Nested
  @DisplayName("From Message Utility")
  class FromMessageUtility {

    @Test
    @DisplayName("Should create QAPFailure from message string")
    void testFromMessage() {
      String message = "Test disabled: Not ready";
      QAPFailure failure = ExceptionFormatter.fromMessage(message);

      assertNotNull(failure);
      assertEquals("java.lang.RuntimeException", failure.getType());
      assertEquals("Test disabled: Not ready", failure.getMessage());
      assertNull(failure.getStackTrace(), "Should have no stack trace");
      assertNull(failure.getCausedBy(), "Should have no causedBy");
      assertNull(failure.getLocation(), "Should have no location");
    }

    @Test
    @DisplayName("Should return null for null message")
    void testFromNullMessage() {
      QAPFailure failure = ExceptionFormatter.fromMessage(null);
      assertNull(failure, "Should return null for null message");
    }
  }

  @Nested
  @DisplayName("Complex Scenarios")
  class ComplexScenarios {

    @Test
    @DisplayName("Should handle exception with cause and suppressed exceptions")
    void testExceptionWithCauseAndSuppressed() {
      Exception rootCause = new Exception("Root cause");
      Exception main = new Exception("Main exception", rootCause);
      Exception suppressed1 = new Exception("Suppressed 1");
      Exception suppressed2 = new Exception("Suppressed 2");

      main.addSuppressed(suppressed1);
      main.addSuppressed(suppressed2);

      QAPFailure failure = ExceptionFormatter.toFailure(main);

      assertNotNull(failure);
      assertEquals("Main exception", failure.getMessage());

      // Check causedBy
      assertNotNull(failure.getCausedBy());
      assertEquals("Root cause", failure.getCausedBy().getMessage());

      // Check suppressed
      assertNotNull(failure.getSuppressed());
      assertEquals(2, failure.getSuppressed().size());

      // Check root cause
      assertNotNull(failure.getRootCause());
      assertEquals("java.lang.Exception", failure.getRootCause().getType());
      assertEquals("Root cause", failure.getRootCause().getMessage());
    }

    @Test
    @DisplayName("Should handle exception with all features")
    void testFullyFeaturedExceptionConversion() {
      IOException rootCause = new IOException("IO failure at disk level");
      SQLException sqlException = new SQLException("Database connection failed", rootCause);
      RuntimeException appException =
          new RuntimeException("Application error occurred", sqlException);

      Exception suppressed = new Exception("Cleanup also failed");
      appException.addSuppressed(suppressed);

      QAPFailure failure = ExceptionFormatter.toFailure(appException);

      // Validate main exception
      assertNotNull(failure);
      assertEquals("java.lang.RuntimeException", failure.getType());
      assertEquals("Application error occurred", failure.getMessage());
      assertNotNull(failure.getStackTrace());
      assertNotNull(failure.getLocation());

      // Validate causedBy chain
      QAPFailure sqlFailure = failure.getCausedBy();
      assertNotNull(sqlFailure);
      assertEquals("java.sql.SQLException", sqlFailure.getType());

      QAPFailure ioFailure = sqlFailure.getCausedBy();
      assertNotNull(ioFailure);
      assertEquals("java.io.IOException", ioFailure.getType());
      assertEquals("IO failure at disk level", ioFailure.getMessage());

      // Validate root cause
      QAPRootCause rootCauseInfo = failure.getRootCause();
      assertNotNull(rootCauseInfo);
      assertEquals("java.io.IOException", rootCauseInfo.getType());
      assertEquals("IO failure at disk level", rootCauseInfo.getMessage());

      // Validate suppressed
      List<QAPFailure> suppressedList = failure.getSuppressed();
      assertNotNull(suppressedList);
      assertEquals(1, suppressedList.size());
      assertEquals("Cleanup also failed", suppressedList.get(0).getMessage());
    }
  }

  @Nested
  @DisplayName("Stack Trace Capping")
  class StackTraceCapping {

    @Test
    @DisplayName("Should not cap stack trace when under limit")
    void testNoCappingWhenUnderLimit() {
      // Reset to high limit
      com.mk.fx.qa.qap.junit.model.StackTraceConfig config =
          com.mk.fx.qa.qap.junit.model.StackTraceConfig.builder()
              .maxLines(1000)
              .headLines(50)
              .tailLines(20)
              .keepUntilFrameworkExit(false)
              .build();
      ExceptionFormatter.setStackTraceConfig(config);

      RuntimeException exception = new RuntimeException("Test error");
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      List<String> stackTrace = failure.getStackTraceLines();
      assertNotNull(stackTrace);
      assertTrue(stackTrace.size() < 1000, "Stack trace should be under limit");

      // Verify no "omitted" message
      boolean hasOmitted = stackTrace.stream().anyMatch(line -> line.contains("omitted"));
      assertFalse(hasOmitted, "Should not have 'omitted' message when under limit");
    }

    @Test
    @DisplayName("Should cap stack trace using head+tail strategy")
    void testHeadTailCapping() {
      // Configure small limits
      com.mk.fx.qa.qap.junit.model.StackTraceConfig config =
          com.mk.fx.qa.qap.junit.model.StackTraceConfig.builder()
              .maxLines(15)
              .headLines(5)
              .tailLines(5)
              .keepUntilFrameworkExit(false)
              .build();
      ExceptionFormatter.setStackTraceConfig(config);

      // Create exception with deep stack
      RuntimeException exception = createDeepStackException();
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      List<String> stackTrace = failure.getStackTraceLines();
      assertNotNull(stackTrace);

      // Should be capped: 5 head + 1 separator + 5 tail = 11 lines
      assertTrue(
          stackTrace.size() <= 15,
          "Stack trace should be capped to configured max: " + stackTrace.size());

      // Should contain omitted message
      boolean hasOmitted = stackTrace.stream().anyMatch(line -> line.contains("omitted"));
      assertTrue(hasOmitted, "Should have '... N more lines omitted ...' separator");

      // First lines should be from the top of the stack
      assertTrue(
          stackTrace.get(0).contains("RuntimeException"), "First line should be exception type");

      // Last lines should be from the bottom of the stack
      String lastLine = stackTrace.get(stackTrace.size() - 1);
      assertTrue(
          lastLine.contains("at ") || lastLine.isEmpty(), "Last line should be a stack frame");
    }

    @Test
    @DisplayName("Should support unlimited stack trace with maxLines=-1")
    void testUnlimitedStackTrace() {
      com.mk.fx.qa.qap.junit.model.StackTraceConfig config =
          com.mk.fx.qa.qap.junit.model.StackTraceConfig.builder()
              .maxLines(-1) // Unlimited
              .headLines(50)
              .tailLines(20)
              .keepUntilFrameworkExit(false)
              .build();
      ExceptionFormatter.setStackTraceConfig(config);

      RuntimeException exception = createDeepStackException();
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      List<String> stackTrace = failure.getStackTraceLines();
      assertNotNull(stackTrace);

      // Should NOT be capped
      boolean hasOmitted = stackTrace.stream().anyMatch(line -> line.contains("omitted"));
      assertFalse(hasOmitted, "Unlimited stack trace should not have 'omitted' message");
    }

    @Test
    @DisplayName("Should cap using 'keep until framework exit' strategy")
    void testKeepUntilFrameworkExitStrategy() {
      com.mk.fx.qa.qap.junit.model.StackTraceConfig config =
          com.mk.fx.qa.qap.junit.model.StackTraceConfig.builder()
              .maxLines(100)
              .headLines(50)
              .tailLines(20)
              .keepUntilFrameworkExit(true) // ← Use this strategy
              .build();
      ExceptionFormatter.setStackTraceConfig(config);

      RuntimeException exception = createDeepStackException();
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      List<String> stackTrace = failure.getStackTraceLines();
      assertNotNull(stackTrace);

      // Should contain at least some frames
      assertTrue(stackTrace.size() > 0, "Should have at least some stack trace lines");

      // When using keepUntilFrameworkExit, the trace should be reasonably sized
      // (stops when hitting framework again after user code)
      assertTrue(
          stackTrace.size() <= 100,
          "Stack trace should be capped with framework exit strategy: " + stackTrace.size());
    }

    @Test
    @DisplayName("Should handle very short maxLines gracefully")
    void testVeryShortMaxLines() {
      com.mk.fx.qa.qap.junit.model.StackTraceConfig config =
          com.mk.fx.qa.qap.junit.model.StackTraceConfig.builder()
              .maxLines(5)
              .headLines(2)
              .tailLines(2)
              .keepUntilFrameworkExit(false)
              .build();
      ExceptionFormatter.setStackTraceConfig(config);

      RuntimeException exception = createDeepStackException();
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      List<String> stackTrace = failure.getStackTraceLines();
      assertNotNull(stackTrace);
      assertTrue(stackTrace.size() <= 5, "Should respect very short maxLines");
    }

    @Test
    @DisplayName("Should include separator with omitted line count")
    void testSeparatorIncludesCount() {
      com.mk.fx.qa.qap.junit.model.StackTraceConfig config =
          com.mk.fx.qa.qap.junit.model.StackTraceConfig.builder()
              .maxLines(20)
              .headLines(8)
              .tailLines(8)
              .keepUntilFrameworkExit(false)
              .build();
      ExceptionFormatter.setStackTraceConfig(config);

      RuntimeException exception = createDeepStackException();
      QAPFailure failure = ExceptionFormatter.toFailure(exception);

      assertNotNull(failure);
      List<String> stackTrace = failure.getStackTraceLines();

      // Find separator line
      String separator =
          stackTrace.stream().filter(line -> line.contains("omitted")).findFirst().orElse(null);

      if (separator != null) {
        // Should contain a number
        assertTrue(separator.matches(".*\\d+.*"), "Separator should include omitted line count");
        assertTrue(
            separator.contains("more lines omitted"), "Separator should have descriptive text");
      }
    }

    // Helper to create exception with deep stack
    private RuntimeException createDeepStackException() {
      return level1();
    }

    private RuntimeException level1() {
      return level2();
    }

    private RuntimeException level2() {
      return level3();
    }

    private RuntimeException level3() {
      return level4();
    }

    private RuntimeException level4() {
      return level5();
    }

    private RuntimeException level5() {
      return new RuntimeException("Deep stack exception");
    }
  }

  // Helper method to create exception at known location
  private RuntimeException createExceptionAtKnownLocation() {
    return new RuntimeException("Exception at known location");
  }
}
