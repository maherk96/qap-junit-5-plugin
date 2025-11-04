
import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(QAPJunitExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // keeps BeforeAll/AfterAll in instance scope
class NestedTestsExample {

    @BeforeAll
    void beforeAll() {
        System.out.println("Before all tests in top-level class");
    }

    @AfterAll
    void afterAll() {
        System.out.println("After all tests in top-level class");
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("Before each test in top-level class");
    }

    @AfterEach
    void afterEach() {
        System.out.println("After each test in top-level class");
    }

    @Test
    @DisplayName("Simple top-level test")
    void topLevelTest() {
        assertTrue(1 + 1 == 2);
    }

    @Nested
    class FirstNested {

        @BeforeEach
        void beforeEachNested() {
            System.out.println("Before each test in FirstNested");
        }

        @AfterEach
        void afterEachNested() {
            System.out.println("After each test in FirstNested");
        }

        @Test
        @DisplayName("Test inside FirstNested")
        void nestedTestOne() {
            assertEquals("hello".toUpperCase(), "HELLO");
        }

        @Nested
        class SecondLevelNested {

            @Test
            @DisplayName("Deeply nested test")
            void deeplyNestedTest() {
                assertNotNull("Deep check");
            }

            @ParameterizedTest(name = "Run {index} with value={0}")
            @ValueSource(strings = {"A", "B"})
            @DisplayName("Parameterized test in SecondLevelNested")
            void parameterizedTest(String input) {
                assertFalse(input.isEmpty());
            }
        }
    }
}