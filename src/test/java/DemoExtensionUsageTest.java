import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("DemoSuite")
@ExtendWith(QAPJunitExtension.class)
@DisplayName("Demo QAP Extension Usage")
public class DemoExtensionUsageTest {

    // --- Normal tests -------------------------------------------------------

    @Test
    @DisplayName("Should run a normal test with display name")
    @Tag("Normal")
    void normalTestWithDisplayName() {
        Assertions.assertTrue(true);
    }

    @Test
    @Tag("NormalNoDN")
    void normalTestWithoutDisplayName() {
        Assertions.assertEquals(2, 1 + 1);
    }

    // --- Parameterized tests -----------------------------------------------

    @ParameterizedTest(name = "Run {index}: {0} + {1} = {2}")
    @CsvSource({
            "1, 2, 3",
            "5, 7, 12"
    })
    @DisplayName("Addition works")
    @Tag("ParamTest")
    void parameterizedAddition(int a, int b, int expected) {
        Assertions.assertEquals(expected, a + b);
    }

    @ParameterizedTest
    @CsvSource({
            "hello, 5",
            "xyz, 3"
    })
    @Tag("ParamNoDN")
    void parameterizedWithoutDisplayName(String s, int len) {
        Assertions.assertEquals(len, s.length());
    }

    // --- Nested tests -------------------------------------------------------

    @Nested
    @Tag("Outer")
    @DisplayName("Math Group")
    class MathGroup {

        @Test
        @DisplayName("Multiplication works")
        @Tag("NestedMethod")
        void multiply() {
            Assertions.assertEquals(12, 3 * 4);
        }

        @Nested
        @Tag("InnerTag")
        class InnerGroup {

            @Test
            @Tag("InnerMethod")
            void innerWithoutDisplayName() {
                Assertions.assertTrue(10 > 2);
            }
        }
    }
}

