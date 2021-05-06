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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class YamalArgumentsProvider implements ArgumentsProvider {
    private Yaml yaml = new Yaml();

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        final var testMethod = extensionContext.getRequiredTestMethod();
        final var testClass = testMethod.getDeclaringClass();

        final YamlSource yamlSource = getYamlSourceAnnotation(testMethod, testClass);
        final Jamal jamal = getJamalAnnotation(testMethod, testClass, yamlSource);
        final var resourceName = yamlSource.value().length() == 0 ? testMethod.getName() + defaultExtension(jamal) : yamlSource.value();

        final Object parameters = getParameters(testClass, jamal, resourceName);

        final var list = new ArrayList<Arguments>();
        try {
            for (final var testYaml : ((Map<String, Map<String, Object>>) parameters).entrySet()) {
                final var displayName = testYaml.getKey();
                Object[] para = new Object[testMethod.getParameters().length];
                int i = 0;
                for (final var parameter : testMethod.getParameters()) {
                    final var name = findAnnotation(parameter, Name.class).map(Name::value)
                        .orElseGet(() -> parameter.getType().getSimpleName());
                    if ("DisplayName".equals(name)) {
                        if (parameter.getType() == DisplayName.class) {
                            para[i] = new DisplayName(displayName);
                        } else {
                            para[i] = displayName;
                        }
                    } else {
                        para[i] = testYaml.getValue().get(name);
                    }
                    i++;
                }
                list.add(Arguments.of(para));
            }
        } catch (ClassCastException cce) {
            throw new ExtensionConfigurationException(format("The YAML source '%s' is not valid.", resourceName));
        }

        return list.stream();
    }

    private Object getParameters(Class<?> testClass, Jamal jamal, String resourceName) throws URISyntaxException {
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

    private String processWithJamal(Jamal jamal, File file, StringBuilder sb) throws BadSyntax {
        final var path = file.getPath();
        final String processed;
        if (jamal.enabled()) {
            final var processor = new Processor(jamal.open(), jamal.close());
            processed = processor.process(Input.makeInput(sb.toString(), new Position(path)));
            if (jamal.dump().length() > 0) {
                final var out = new File(file.getParentFile(), jamal.dump());
                try (final var fos = new FileOutputStream(out)) {
                    fos.write(processed.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new ExtensionConfigurationException(format("Cannot write the dump file '%s'.", jamal.dump()), e);
                }
            }
        } else {
            processed = sb.toString();
        }
        return processed;
    }

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
     * This argument provider can only be used with test methods, which are annotated with {@link YamlSource} or are in
     * a class, which is annotated with {@link YamlSource}.
     * <p>
     * If the annotation is present on both the class and the method then the one on the method prevails
     *
     * @param testMethod the test method
     * @param testClass  the class that the test method is declared in
     * @return the annotation object that controls the execution of this run of this argument provider
     */
    private YamlSource getYamlSourceAnnotation(java.lang.reflect.Method testMethod, Class<?> testClass) {
        final var yamlSource = findAnnotation(testMethod, YamlSource.class)
            .orElseGet(() -> findAnnotation(testClass, YamlSource.class)
                .orElseThrow(() -> new ExtensionConfigurationException(format("Test method is not annotated with '@%s'",
                    YamlSource.class.getSimpleName()))));
        return yamlSource;
    }


}
