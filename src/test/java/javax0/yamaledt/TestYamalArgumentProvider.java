package javax0.yamaledt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;

import static java.lang.String.format;

public class TestYamalArgumentProvider {

    /**
     * This test demonstrates how you can pass the DisplayName for each test using the {@link Name} annotation.
     *
     * @param dn the display name of the test
     */
    @ParameterizedTest(name = "{0}")
    @YamlSource(jamal = @Jamal(enabled = false))
    void testDisplayNameOnly(@Name("DisplayName") CharSequence dn) {

    }

    /**
     * This test demonstrates how you can pass the DisplayName for each test when the name is identified by the class.
     * {@link DisplayName} is defined in the library, and it can be used to save typing the annotation on the parameter.
     *
     * @param dn is just the display name
     */
    // snippet testDisplayNameOnly1
    @ParameterizedTest(name = "{0}")
    @YamlSource("testDisplayNameOnly.yaml.jam")
    void testDisplayNameOnly1(DisplayName dn) {
        // ... actual test ...
    }
    // end snippet

    @ParameterizedTest(name = "{0}")
    @YamlSource("test1:\n" +
        "test2:\n" +
        "test3:\n")
    void testWithDataInTheAnnotation(DisplayName dn) {
        // ... actual test ...
    }

    @ParameterizedTest(name = "{0}")
    @YamlSource(value = "test:\n" +
        "  test1:\n" +
        "  test2:\n" +
        "  test3:\n", ognl = "test")
    void testWithDataInTheAnnotationAndOgnl(DisplayName dn) {
        // ... actual test ...
    }

    // snippet sampleTestWithSimpleParameters
    @ParameterizedTest(name = "{0}")
    @YamlSource()
    void sampleTestWithSimpleParameters(@Name("DisplayName") String dn,
                                        int i,
                                        @Name("k") int k) {
        Assertions.assertEquals(5, i + k);
    }
    // end snippet

    @ParameterizedTest(name = "{0}")
    @Jamal(enabled = false)
    @YamlSource
    void sampleTestWithObjectParameters(@Name("DisplayName") String dn,
                                        int i,
                                        @Name("k") Integer k) {
        Assertions.assertEquals(5, i + (k == null ? 0 : k));
    }


    @Jamal(enabled = false)
    @ParameterizedTest(name = "{0}")
    @YamlSource
    void testCustomClassParameter(@Name("DisplayName") String dn, CustomClass customer, @Name("result") String r) {
        Assertions.assertEquals(r, format("%d.%s.%s", customer.serial, customer.name, customer.weight));
    }

    @ParameterizedTest(name = "{0}")
    @Jamal(dump = "testCustomClassParameterWithJamal.yaml")
    @YamlSource("testCustomClassParameterWithJamal.yaml")
    void testCustomClassParameter1(@Name("DisplayName") String dn, CustomClass customer, @Name("result") String r) {
        Assertions.assertEquals(r, format("%d.%s.%s", customer.serial, customer.name, customer.weight));
    }

    @YamlSource("Shared.yaml")
    @Nested
    class TestInner {
        @Jamal(enabled = false)
        @Nested
        class TestInnerInner {

            @ParameterizedTest(name = "{0}")
            @YamlSource(ognl = "test1")
            @org.junit.jupiter.api.DisplayName("Inner Inner Concatenate")
            void test1(@Name("DisplayName") String dn, @Name("a") String a, @Name("b") String b, @Name("concat") String concat) {
                Assertions.assertEquals(concat, a + b);
            }

            @ParameterizedTest(name = "{0}")
            @YamlSource(ognl = "test2")
            @org.junit.jupiter.api.DisplayName("Inner Inner . Concatenate")
            void test2(@Name("DisplayName") String dn, @Name("a") String a, @Name("b") String b, @Name("concat") String concat) {
                Assertions.assertEquals(concat, a + "."+b);
            }
        }
    }

}
