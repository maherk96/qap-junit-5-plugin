package com.mk.fx.qa.qap.junit.extension;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mk.fx.qa.qap.junit.annotation.CoverageItem;
import com.mk.fx.qa.qap.junit.extension.support.InMemoryStore;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

class QAPJunitCoverageItemCaptureTest {

  static class AnnotatedTestClass {
    @CoverageItem("METHOD-COV-200")
    @CoverageItem("METHOD-COV-201")
    @DisplayName("Coverage Test Method")
    void someTest() {}
  }

  @Test
  void methodCoverageItems_populated_from_method_annotations() throws Exception {
    // Arrange context and in-memory store
    ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
    ExtensionContext root = mock(ExtensionContext.class);
    when(ctx.getRoot()).thenReturn(root);
    InMemoryStore store = new InMemoryStore();
    when(root.getStore(any())).thenReturn(store);

    when(ctx.getRequiredTestClass()).thenReturn((Class) AnnotatedTestClass.class);
    when(ctx.getTestClass()).thenReturn(Optional.of(AnnotatedTestClass.class));

    Method m = AnnotatedTestClass.class.getDeclaredMethod("someTest");
    when(ctx.getRequiredTestMethod()).thenReturn(m);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));
    when(ctx.getDisplayName()).thenReturn("someTest()");

    QAPJunitExtension ext = new QAPJunitExtension();

    // Act
    ext.beforeEach(ctx);

    // Assert
    QAPTest q = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
    assertNotNull(q);
    assertEquals(2, q.getCoverageItems().size());
    assertTrue(q.getCoverageItems().contains("METHOD-COV-200"));
    assertTrue(q.getCoverageItems().contains("METHOD-COV-201"));
    assertEquals("Coverage Test Method", q.getMethodDisplayName());
  }

  @Test
  void coverageItems_and_tags_work_together() throws Exception {
    // Test class with both tags and coverage items
    @org.junit.jupiter.api.Tag("test-tag")
    class MixedAnnotations {
      @org.junit.jupiter.api.Tag("method-tag")
      @CoverageItem("COV-456")
      void mixedMethod() {}
    }

    ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
    ExtensionContext root = mock(ExtensionContext.class);
    when(ctx.getRoot()).thenReturn(root);
    InMemoryStore store = new InMemoryStore();
    when(root.getStore(any())).thenReturn(store);

    when(ctx.getRequiredTestClass()).thenReturn((Class) MixedAnnotations.class);
    when(ctx.getTestClass()).thenReturn(Optional.of(MixedAnnotations.class));

    Method m = MixedAnnotations.class.getDeclaredMethod("mixedMethod");
    when(ctx.getRequiredTestMethod()).thenReturn(m);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));
    when(ctx.getDisplayName()).thenReturn("mixedMethod()");

    QAPJunitExtension ext = new QAPJunitExtension();

    // Act
    ext.beforeEach(ctx);

    // Assert
    QAPTest q = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
    assertNotNull(q);

    // Verify both tags and coverage items are captured
    assertEquals(1, q.getTags().getMethod().size());
    assertTrue(q.getTags().getMethod().contains("method-tag"));
    assertEquals(1, q.getCoverageItems().size());
    assertTrue(q.getCoverageItems().contains("COV-456"));
  }

  @Test
  void empty_coverage_items_when_no_annotations() throws Exception {
    class NoAnnotations {
      void plainMethod() {}
    }

    ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
    ExtensionContext root = mock(ExtensionContext.class);
    when(ctx.getRoot()).thenReturn(root);
    InMemoryStore store = new InMemoryStore();
    when(root.getStore(any())).thenReturn(store);

    when(ctx.getRequiredTestClass()).thenReturn((Class) NoAnnotations.class);
    when(ctx.getTestClass()).thenReturn(Optional.of(NoAnnotations.class));

    Method m = NoAnnotations.class.getDeclaredMethod("plainMethod");
    when(ctx.getRequiredTestMethod()).thenReturn(m);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));
    when(ctx.getDisplayName()).thenReturn("plainMethod()");

    QAPJunitExtension ext = new QAPJunitExtension();

    // Act
    ext.beforeEach(ctx);

    // Assert
    QAPTest q = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
    assertNotNull(q);
    assertNotNull(q.getCoverageItems());
    assertTrue(q.getCoverageItems().isEmpty());
  }
}
