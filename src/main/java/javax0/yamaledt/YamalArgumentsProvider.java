package javax0.yamaledt;

import javax0.jamal.api.BadSyntax;
import javax0.jamal.api.Position;
import javax0.jamal.engine.Processor;
import javax0.jamal.tools.Input;
import javax0.yamaledt.utils.JamalDefault;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class YamalArgumentsProvider implements ArgumentsProvider {
    private Yaml yaml = new Yaml();

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        final var testMethod = extensionContext.getRequiredTestMethod();
        final var testClass = testMethod.getDeclaringClass();

        final YamlSource yamlSource = getYamlSourceAnnotation(testMethod);
        final Jamal jamal = getJamalAnnotation(testMethod, testClass, yamlSource);
        final var resourceName = yamlSource.value().length() == 0 ? testMethod.getName() + defaultExtension(jamal) : yamlSource.value();

        final Map<String, Map<String, Object>> parameters = getParameters(testClass, jamal, resourceName);

        return createArgumentsStream(testMethod, resourceName, parameters, yamlSource.strict());
    }

    /**
     * Create the streams for the arguments.
     *
     * @param testMethod   the method to be tested.
     * @param resourceName the name of the resource from where the parameters were read. It is needed only to report
     *                     error in some exception in case there is some wrong formatting in the Yaml file.
     * @param parameters   the yaml structure holding the test parameters
     * @param strict       check that there are no extra, ignored parameters in the Yaml data set
     * @return the stream of arguments composed
     */
    private Stream<Arguments> createArgumentsStream(java.lang.reflect.Method testMethod, String resourceName, Map<String, Map<String, Object>> parameters, boolean strict) {
        final var list = new ArrayList<Arguments>();
        try {
            final String[] names = getNames(testMethod);
            final var nameSet = Set.of(names);
            for (final var testYaml : parameters.entrySet()) {
                final var displayName = testYaml.getKey();
                Object[] para = new Object[testMethod.getParameters().length];
                for (int i = 0; i < names.length; i++) {
                    final String name = names[i];
                    if ("DisplayName".equals(name)) {
                        setDisplayName(displayName, para, i, testMethod.getParameters()[i].getType() == DisplayName.class);
                    } else {
                        if (strict && !testYaml.getValue().containsKey(name)) {
                            throw new ExtensionConfigurationException(
                                format("The parameter '%s' in the test record '%s' of the test %s::%s() is not defined",
                                    name,
                                    displayName,
                                    testMethod.getDeclaringClass().getName(), testMethod.getName()));
                        }
                        para[i] = testYaml.getValue().get(name);
                    }
                }
                list.add(Arguments.of(para));
                if (strict) {
                    for (final var key : testYaml.getValue().keySet()) {
                        if (!nameSet.contains(key)) {
                            throw new ExtensionConfigurationException(
                                format("There is an extra key '%s' in the test record '%s' of the test %s::%s()",
                                    key,
                                    displayName,
                                    testMethod.getDeclaringClass().getName(), testMethod.getName()));
                        }
                    }
                }
            }
        } catch (ClassCastException cce) {
            throw new ExtensionConfigurationException(format("The YAML source '%s' is not valid.", resourceName));
        }
        return list.stream();
    }

    private String[] getNames(java.lang.reflect.Method testMethod) {
        final String[] names = new String[testMethod.getParameterCount()];
        int displayNameIndex = -1;
        for (int i = 0; i < names.length; i++) {
            final var parameter = testMethod.getParameters()[i];
            names[i] = getName(parameter);
            if (parameter.getType() == DisplayName.class) {
                if (displayNameIndex == -1) {
                    displayNameIndex = i;
                } else {
                    throw new ExtensionConfigurationException(format("The test method %s::%s cannot have more than one parameter of the type %s",
                        testMethod.getDeclaringClass().getName(), testMethod.getName(),
                        DisplayName.class.getSimpleName()));
                }
            }
        }
        return names;
    }

    /**
     * Get the name of the parameter. Since the Java variable name is not available during run-time there are two
     * possibilities. The parameter may be annotated with the annotation {@link Name} or the name of the type of the
     * parameter is used.
     * <p>
     * The second option is a shorthand in case there are no more parameters of the same time and the type of the
     * parameter is expressive enough. In other cases the {@link Name} annotation should be used.
     *
     * @param parameter the parameter of which we want the name of
     * @return the name of the parameter
     */
    private String getName(java.lang.reflect.Parameter parameter) {
        final var name = findAnnotation(parameter, Name.class).map(Name::value)
            .orElseGet(() -> parameter.getType().getSimpleName());
        return name;
    }

    /**
     * Set the parameter to be the display name for this test.
     *
     * @param displayName         if the value of the display name to be displayed when the test is running with these
     *                            parameters
     * @param para                the array of the parameters
     * @param i                   this is the index in the parameter array to set the value to
     * @param useDisplayNameClass signals that the we have to create a {@link DisplayName} object containing the {@code
     *                            displayName} string. If it is {@code false} then the string from the Yaml file will
     *                            directly be used.
     */
    private void setDisplayName(String displayName, Object[] para, int i, boolean useDisplayNameClass) {
        if (useDisplayNameClass) {
            para[i] = new DisplayName(displayName);
        } else {
            para[i] = displayName;
        }
    }

    /**
     * Get the test parameters in an Object as read from the Yaml/Jamal file.
     *
     * @param testClass    the class that the test method is in. This is used to identify the location of the resource.
     *                     The test data file is read from the same directory where the class is.
     * @param jamal        the Jamal annotation. It is used to get access to the Jamal parameters (e.g.: macro opening
     *                     and closing strings) as well as to know if Jamal processing is enabled.
     * @param resourceName the name of the resource file that contains the Yaml/Jamal formatted parameters
     * @return the Yaml structure read from the file
     * @throws URISyntaxException if the file cannot be identified
     */
    private Map<String, Map<String, Object>> getParameters(Class<?> testClass, Jamal jamal, String resourceName) throws URISyntaxException {
        final StringBuilder sb = readResource(testClass, resourceName);
        try {
            final var file = Paths.get(testClass.getResource(resourceName).toURI()).toFile().getAbsoluteFile();
            final String processed = processWithJamal(jamal, file, sb);
            return yaml.load(processed);
        } catch (BadSyntax e) {
            throw new ExtensionConfigurationException(format("The source '%s' is not a valid Jamal source.", resourceName), e);
        } catch (ParserException e) {
            throw new ExtensionConfigurationException(format("The Yaml file '%s' is erroneous.", resourceName), e);
        }
    }

    /**
     * Reads the content of the resource.
     *
     * @param testClass    the class that contains the test methof. The resource will be read from the same
     *                     package/directory where the class file is.
     * @param resourceName the local name of the resource. It is relative to the class name.
     * @return the content of the resource as a StringBuilder.
     */
    private StringBuilder readResource(Class<?> testClass, String resourceName) {
        final StringBuilder sb = new StringBuilder();
        try (final var is = testClass.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new ExtensionConfigurationException(format("The source '%s' is not found.", resourceName));
            }
            try (InputStreamReader isReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isReader)) {
                String str;
                while ((str = reader.readLine()) != null) {
                    sb.append(str).append("\n");
                }
            }
        } catch (IOException ioe) {
            throw new ExtensionConfigurationException(format("The source '%s' is not readable.", resourceName), ioe);
        }
        return sb;
    }

    /**
     * Process the input using Jamal. The processing takes only place when Jamal processing is enabled. If Jamal
     * processing is not enabled then the return value is the same as the input in {@code sb}.
     * <p>
     * The code also dumps the result into a file in case the Jamal annotation defines a dump file.
     *
     * @param jamal is the annotation instance that tells if Jamal processing is enabled
     * @param file  the file where the resource is. It is used to calculate the absolute path that can be used by the
     *              Jamal processor in case the Jamal source file is including or importing some other files. It may not
     *              work in case the resource is inside a JAR file. This parameter is also used to identify the
     *              directory where the input file is. This directory is used when the Jamal output is to be dumped for
     *              debugging purpose.
     * @param sb    the input already read from the resource file through an input stream. This works even if the file
     *              is inside a JAR file.
     * @return the processed string that is already YAML format (hopefully)
     * @throws BadSyntax if there is something wrong while processing the text using Jamal.
     */
    private String processWithJamal(Jamal jamal, File file, StringBuilder sb) throws BadSyntax {
        final var path = file.getPath();
        final String processed;
        if (jamal.enabled()) {
            try (final var processor = new Processor(jamal.open(), jamal.close())) {
                processed = processor.process(Input.makeInput(sb.toString(), new Position(path)));
                if (jamal.dump().length() > 0) {
                    final var out = new File(file.getParentFile(), jamal.dump());
                    try (final var fos = new FileOutputStream(out)) {
                        fos.write(processed.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new ExtensionConfigurationException(format("Cannot write the dump file '%s'.", jamal.dump()), e);
                    }
                }
            }
        } else {
            processed = sb.toString();
        }
        return processed;
    }

    /**
     * The default extension is {@code .yaml.jam} in case Jamal is enabled, which is the default. If Jamal is disabled
     * then the default extension is {@code .yaml}.
     *
     * @param jamal the annotation telling if Jamal processing is enabled
     * @return the default extension for the input file in case there is no file specified and the resource name is
     * calculated from the name of the test method
     */
    private String defaultExtension(Jamal jamal) {
        return jamal.enabled() ? ".yaml.jam" : ".yaml";
    }

    /**
     * Jamal processing of the test source is switched on by default. It can be switched off using the annotation {@link
     * Jamal}. This annotation can also provide configuration options for the Jamal processing, like the opening and
     * closing string for macros can be defined.
     * <p>
     * The annotation can be on the method, on the class that contains the test method or be specified as the parameter
     * {@code jamal} of the annotation {@link YamlSource}. This is also the order how the method looks for this
     * annotation.
     *
     * @param testMethod       the test method, which may be annotated with @{@link Jamal}
     * @param testClass        the class that may also be annotated with @{@link Jamal}
     * @param methodAnnotation the YamlSource annotation instance that may have jamalJamal parameter.
     * @return a @{@link Jamal} annotation instance (may be the the default if not specified anywhere).
     */
    private Jamal getJamalAnnotation(java.lang.reflect.Method testMethod, Class<?> testClass, YamlSource methodAnnotation) {
        final var jamalOnMethod = findAnnotation(testMethod, Jamal.class);
        final var jamalOnClass = findAnnotation(testClass, Jamal.class);
        final var jamalInAnnotation = methodAnnotation.jamal();
        final var jamalInAnnotationIsDefault = jamalInAnnotation.open().equals("\u0000");
        if (jamalOnMethod.isPresent() && !jamalInAnnotationIsDefault) {
            throw new ExtensionConfigurationException(
                format("You cannot have a @%s annotation on the method and also on the 'jamal' parameter of the @%s annotation on the test method '%s::%s'.",
                    Jamal.class.getSimpleName(),
                    YamlSource.class.getSimpleName(),
                    testClass.getName(), testMethod.getName()));
        }
        final var jamal = jamalOnMethod
            .orElseGet(() -> (jamalInAnnotationIsDefault ? jamalOnClass : Optional.of(jamalInAnnotation))
                .orElseGet(() -> new JamalDefault()));
        return jamal;
    }

    /**
     * This argument provider can only be used with test methods, which are annotated with {@link YamlSource}.
     *
     * @param testMethod the test method
     * @return the annotation object that controls the execution of this run of this argument provider
     */
    private YamlSource getYamlSourceAnnotation(Method testMethod) {
        final var yamlSource = findAnnotation(testMethod, YamlSource.class)
            .orElseThrow(() -> new ExtensionConfigurationException(format("Test method is not annotated with '@%s'",
                YamlSource.class.getSimpleName())));
        return yamlSource;
    }


}
