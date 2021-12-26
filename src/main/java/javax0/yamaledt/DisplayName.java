package javax0.yamaledt;

/**
 * This is wrapper class around a {@code String} value to be used as the type of the display name parameter in
 * parametrized tests annotated with {@link YamlSource @YamlSource} annotation.
 * <p>
 * Tests are named with a meaningful string in the Yaml file.
 *
 * <pre>
 * {@jamal {@include [verbatim] ../../../../../src/test/resources/javax0/yamaledt/sampleTestWithObjectParameters.yaml}}
 * </pre>
 *
 * In the example above, the test is named "adding null to five is five", and "adding zero to five is five".
 * These strings will be passed to test method parameters as values, and they will be used by the JUnit5 framework as
 * {@link org.junit.jupiter.api.DisplayName}.
 *
 * In the case of parametrized tests creating the test display name is a two-step process:
 *
 * 1. You have to specify a {@code name} parameter for the {@code ParameterizedTest} annotation.
 * This parameter will contain a formatting string with placeholders. That way you can fabricate a meaningful test name
 * that contains some parameters. For more information have a look at the documentation of the
 * {@link org.junit.jupiter.params.ParameterizedTest} annotation.
 *
 * 2. The display name, which is the name of the test in our case is injected into one of the parameters.
 *
 * When the {@code name} format string refers to this parameter, then the name of the test will be the content of this
 * parameter.
 * <p>
 * When the parameter source for the parametrized test is given from a Yaml file using the annotation {@link
 * YamlSource} the description of the test from the Yaml data structure is moved to the parameter, which is "named"
 * {@code DisplayName}. This is not the real name of the method parameter, as you can see in the source code, because
 * that information is not available at runtime.
 *
 * The name of the parameter either comes as the value of the {@link Name} annotation or the name of the
 * type of the parameter. To name a parameter that way you can write
 *
 * <pre>{@code
 *   void testDisplayNameOnly(@Name("DisplayName") CharSequence dn)
 * }</pre>
 *
 * The name {@code dn} is only in the source code and not in the byte code, not available for the test framework,
 * unfortunately.
 * <p>
 * To simplify the specification of the parameter, which plays the role of the display name and to eliminate the need to
 * insert the {@code Name("DisplayName")} in front of the parameter here is a type, which is named {@code DisplayName}.
 * <p>
 * The following example is from the unit tests of the project and demonstrates the use of this type:
 * <p>
 * {@jamal {@snip:collect onceAs="YamlSourceTest" from="../../../../test/java"}
 * <pre>
 * {@snip testDisplayNameOnly1}
 * </pre>
 * }
 *
 * In this example one of the parameters (well, there is only one parameter) has the type {@code DisplayName} and thus
 * it is used as display name. Use this type to specify the argument, which is the display name, specify the position in
 * the {@code name} parameter of the annotation {@code @ParameterizedTest} and please put it on the first position as in
 * the example unless you are your own greatest enemy. Do not instantiate or use this class in any other way, please.
 */
public class DisplayName {
    private final String value;

    /**
     * Used in the class of {@link YamalArgumentsProvider}. Do not use it anywhere else.
     * @param value the string, which is the display name of the test
     */
    public DisplayName(String value) {
        this.value = value;
    }

    /**
     * Invoked by the JUnit 5 framework when it wants to display the display name.
     * @return the string rep of the display name
     */
    @Override
    public String toString() {
        return value;
    }
}
