package com.mk.fx.qa.qap.junit.extension;

import com.mk.fx.qa.qap.junit.util.TagExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tag Collector Unit Tests")
class TagCollectorTest {

    // --- Fixtures -----------------------------------------------------------

    @Tag("ClassTag")
    static class ClassTagged {}

    static class NoTagsClass {}

    static class MethodTagged {
        @Tag("MethodTag")
        void hasTag() {}

        void noTag() {}

        @Tag("Tag1")
        @Tag("Tag2")
        void multipleTags() {}
    }

    @Tag("ClassLevel")
    static class BothLevels {
        @Tag("MethodLevel")
        void taggedMethod() {}
    }

    // Real nested structure for inheritance testing
    @Tag("ParentTag")
    static class RealParent {
        @Tag("ChildTag")
        static class RealChild {}
    }

    // 3-level nesting
    @Tag("TopTag")
    static class Top {
        @Tag("MidTag")
        static class Mid {
            @Tag("LeafTag")
            static class Leaf {}
        }
    }

    @Tag("DupeTag")
    static class DuplicateTags {
        @Tag("DupeTag")
        void method() {}
    }

    // --- Tests: Method Tags -------------------------------------------------

    @Test
    @DisplayName("Should collect method-level tags")
    void shouldCollectMethodTags() throws Exception {
        Method m = MethodTagged.class.getDeclaredMethod("hasTag");
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));

        Set<String> tags = TagExtractor.methodTags(ctx);
        assertEquals(Set.of("MethodTag"), tags);
    }

    @Test
    @DisplayName("Should handle methods with no tags")
    void shouldHandleMethodsWithNoTags() throws Exception {
        Method m = MethodTagged.class.getDeclaredMethod("noTag");
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));

        Set<String> tags = TagExtractor.methodTags(ctx);
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple tags on same method")
    void shouldHandleMultipleTags() throws Exception {
        Method m = MethodTagged.class.getDeclaredMethod("multipleTags");
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));

        Set<String> tags = TagExtractor.methodTags(ctx);
        assertEquals(Set.of("Tag1", "Tag2"), tags);
    }

    @Test
    @DisplayName("Should handle missing test method")
    void shouldHandleEmptyTestMethod() {
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestMethod()).thenReturn(Optional.empty());

        Set<String> tags = TagExtractor.methodTags(ctx);
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    // --- Tests: Class Tags --------------------------------------------------

    @Test
    @DisplayName("Should collect class-level tags")
    void shouldCollectClassTags() {
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestClass()).thenReturn(Optional.of(ClassTagged.class));

        Set<String> tags = TagExtractor.classTags(ctx);
        assertEquals(Set.of("ClassTag"), tags);
    }

    @Test
    @DisplayName("Should handle classes with no tags")
    void shouldHandleClassesWithNoTags() {
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestClass()).thenReturn(Optional.of(NoTagsClass.class));

        Set<String> tags = TagExtractor.classTags(ctx);
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("Should handle missing test class")
    void shouldHandleEmptyTestClass() {
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestClass()).thenReturn(Optional.empty());

        Set<String> tags = TagExtractor.classTags(ctx);
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    // --- Tests: Method & Class Tags Together --------------------------------

    @Test
    @DisplayName("Should distinguish between method and class tags")
    void shouldDistinguishMethodAndClassTags() throws Exception {
        Method m = BothLevels.class.getDeclaredMethod("taggedMethod");
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));
        when(ctx.getTestClass()).thenReturn(Optional.of(BothLevels.class));

        Set<String> methodTags = TagExtractor.methodTags(ctx);
        Set<String> classTags = TagExtractor.classTags(ctx);

        assertEquals(Set.of("MethodLevel"), methodTags);
        assertEquals(Set.of("ClassLevel"), classTags);
        assertFalse(methodTags.contains("ClassLevel"));
        assertFalse(classTags.contains("MethodLevel"));
    }

    @Test
    @DisplayName("Should handle duplicate tags across method and class")
    void shouldHandleDuplicateTags() throws Exception {
        Method m = DuplicateTags.class.getDeclaredMethod("method");
        ExtensionContext ctx = mock(ExtensionContext.class);
        when(ctx.getTestMethod()).thenReturn(Optional.of(m));
        when(ctx.getTestClass()).thenReturn(Optional.of(DuplicateTags.class));

        Set<String> methodTags = TagExtractor.methodTags(ctx);
        Set<String> classTags = TagExtractor.classTags(ctx);

        assertTrue(methodTags.contains("DupeTag"));
        assertTrue(classTags.contains("DupeTag"));
    }

    // --- Tests: Inherited Tags ----------------------------------------------

    @Test
    @DisplayName("Should collect inherited tags from parent class")
    void shouldCollectInheritedTags() {
        // Use real nested classes for accurate test
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());

        ExtensionContext parent = mock(ExtensionContext.class);
        when(parent.getTestClass()).thenReturn(Optional.of(RealParent.class));
        when(parent.getParent()).thenReturn(Optional.of(root));

        ExtensionContext child = mock(ExtensionContext.class);
        when(child.getTestClass()).thenReturn(Optional.of(RealParent.RealChild.class));
        when(child.getParent()).thenReturn(Optional.of(parent));

        // Direct class tags
        Set<String> classTags = TagExtractor.classTags(child);
        assertEquals(Set.of("ChildTag"), classTags);
        assertFalse(classTags.contains("ParentTag"));

        // Inherited tags
        Set<String> inherited = TagExtractor.inheritedClassTags(child);
        assertTrue(inherited.contains("ParentTag"));
        assertFalse(inherited.contains("ChildTag"));
    }

    @Test
    @DisplayName("Should collect inherited tags from multiple levels")
    void shouldCollectMultiLevelInheritedTags() {
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getParent()).thenReturn(Optional.empty());

        ExtensionContext top = mock(ExtensionContext.class);
        when(top.getTestClass()).thenReturn(Optional.of(Top.class));
        when(top.getParent()).thenReturn(Optional.of(root));

        ExtensionContext mid = mock(ExtensionContext.class);
        when(mid.getTestClass()).thenReturn(Optional.of(Top.Mid.class));
        when(mid.getParent()).thenReturn(Optional.of(top));

        ExtensionContext leaf = mock(ExtensionContext.class);
        when(leaf.getTestClass()).thenReturn(Optional.of(Top.Mid.Leaf.class));
        when(leaf.getParent()).thenReturn(Optional.of(mid));

        Set<String> inherited = TagExtractor.inheritedClassTags(leaf);
        assertTrue(inherited.contains("TopTag"));
        assertTrue(inherited.contains("MidTag"));
        assertFalse(inherited.contains("LeafTag"));
        assertEquals(2, inherited.size());
    }

    @Test
    @DisplayName("Should return empty inherited tags for root class")
    void shouldReturnEmptyInheritedTagsForRoot() {
        ExtensionContext root = mock(ExtensionContext.class);
        when(root.getTestClass()).thenReturn(Optional.of(RealParent.class));
        when(root.getParent()).thenReturn(Optional.empty());

        Set<String> inherited = TagExtractor.inheritedClassTags(root);
        assertNotNull(inherited);
        assertTrue(inherited.isEmpty());
    }
}