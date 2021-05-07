package javax0.yamaledt;

/**
 * This is wrapper class around a {@code String} value to be used as the type of the display name parameter in
 * parametrized tests annotated with {@link YamlSource @YamlSource} annotation.
 * <p>
 * Tests are named providing a meaningful, probably quote string in the Yaml file. The name for each record is used
 * similarly as the name in the {@code DisplayName} annotation in usual JUnit 5 tests. In case of parameterized tests
 * this name has to be assigned to a parameter and then the {@code name} parameter of the annotation {@code
 * ParameterizedTest} points to the parameter, which holds the display name of the test.
 * <p>
 * When the parameter source for the parameterized test is given from a Yaml file using the annotation {@link
 * YamlSource} the description of the test from the Yaml data structure is moved to the parameter, which is named {@code
 * DisplayName}. The name of the parameter either comes as the value of the {@link Name} annotation or the name of the
 * type of the parameter.
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
 * } In this example one of the parameters (well, there is only one parameter) has the type {@code DisplayName} and thus
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
