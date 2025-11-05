package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.extension.support.InMemoryStore;
import com.mk.fx.qa.qap.junit.model.QAPTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import static com.mk.fx.qa.qap.junit.core.QAPUtils.METHOD_DESCRIPTION_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QAPJunitNestedTagsTest {

    @Tag("TopTag")
    @DisplayName("Top")
    static class Top {}

    @Tag("MidTag")
    @DisplayName("Mid")
    static class Mid {}

    @Tag("LeafTag")
    @DisplayName("Leaf")
    static class Leaf {
        @Tag("MethodTag")
        void test() {}
    }

    @Test
    void separates_current_class_tags_from_inherited_enclosing_tags() throws Exception {
        // Mock a nested ExtensionContext chain: top -> mid -> leaf(method)
        ExtensionContext top = mock(ExtensionContext.class);
        ExtensionContext mid = mock(ExtensionContext.class);
        ExtensionContext leaf = mock(ExtensionContext.class, RETURNS_DEEP_STUBS);

        when(leaf.getParent()).thenReturn(Optional.of(mid));
        when(mid.getParent()).thenReturn(Optional.of(top));
        when(top.getParent()).thenReturn(Optional.empty());

        when(leaf.getTestClass()).thenReturn(Optional.of(Leaf.class));
        when(mid.getTestClass()).thenReturn(Optional.of(Mid.class));
        when(top.getTestClass()).thenReturn(Optional.of(Top.class));

        // Required test class/method for stores
        when(leaf.getRequiredTestClass()).thenReturn((Class) Leaf.class);
        Method m = Leaf.class.getDeclaredMethod("test");
        when(leaf.getRequiredTestMethod()).thenReturn(m);
        when(leaf.getTestMethod()).thenReturn(Optional.of(m));
        when(leaf.getDisplayName()).thenReturn("test()");

        // Setup Store
        ExtensionContext root = mock(ExtensionContext.class);
        when(leaf.getRoot()).thenReturn(root);
        when(mid.getRoot()).thenReturn(root);
        when(top.getRoot()).thenReturn(root);
        InMemoryStore store = new InMemoryStore();
        when(root.getStore(any())).thenReturn(store);

        QAPJunitExtension ext = new QAPJunitExtension();
        ext.beforeEach(leaf);

        QAPTest test = store.get(METHOD_DESCRIPTION_KEY, QAPTest.class);
        assertNotNull(test);
        assertEquals(Set.of("MethodTag"), test.getMethodTags());
        assertEquals(Set.of("LeafTag"), test.getClassTags());
        assertEquals(Set.of("TopTag", "MidTag"), test.getInheritedClassTags());
    }
}
