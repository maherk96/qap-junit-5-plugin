package com.mk.fx.qa.qap.junit.extension;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mk.fx.qa.qap.junit.extension.support.InMemoryStore;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

class QAPJunitExtensionDisabledTest {

  @Tag("ClassX")
  static class C {
    @Tag("MethodY")
    @DisplayName("DN")
    void test() {}
  }

  @Test
  void disabled_test_records_reason_and_method_tags_and_added_to_class_store() throws Exception {
    // Arrange context + stores
    ExtensionContext ctx = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);
    ExtensionContext root = mock(ExtensionContext.class);
    when(ctx.getRoot()).thenReturn(root);
    InMemoryStore store = new InMemoryStore();
    when(root.getStore(any())).thenReturn(store);

    when(ctx.getRequiredTestClass()).thenReturn((Class) C.class);
    when(ctx.getTestClass()).thenReturn(Optional.of(C.class));
    Method m = C.class.getDeclaredMethod("test");
    when(ctx.getRequiredTestMethod()).thenReturn(m);
    when(ctx.getTestMethod()).thenReturn(Optional.of(m));
    when(ctx.getDisplayName()).thenReturn("DN");
    when(ctx.getTags()).thenReturn(Set.of("ClassX", "MethodY"));

    QAPJunitExtension ext = new QAPJunitExtension();

    // Act
    ext.testDisabled(ctx, Optional.of("Skip because of maintenance"));

    // Assert the test was appended to class store list
    @SuppressWarnings("unchecked")
    List<QAPTest> cases = store.get(METHOD_DESCRIPTION_KEY, List.class);
    assertNotNull(cases);
    assertEquals(1, cases.size());
    QAPTest t = cases.get(0);
    assertEquals(Set.of("MethodY"), t.getMethodTags());
    assertEquals("DN", t.getMethodDisplayName());
    assertEquals("DISABLED", t.getStatus());
    assertTrue(t.hasException());
    assertTrue(new String(t.getException()).contains("maintenance"));
    assertTrue(t.getEndTime() >= t.getStartTime());
  }
}
