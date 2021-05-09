package javax0.yamaledt;

import javax0.jamal.api.BadSyntax;
import javax0.jamal.api.Position;
import javax0.jamal.engine.Processor;
import javax0.jamal.tools.Input;
import ognl.Ognl;
import ognl.OgnlException;
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class YamalArgumentsProvider implements ArgumentsProvider {
    private Yaml yaml = new Yaml();

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        final var testMethod = extensionContext.getRequiredTestMethod();
        final var testClass = testMethod.getDeclaringClass();

        final YamlSource yamlSource = getYamlSourceAnnotation(testMethod);
        final Jamal jamal = getJamalAnnotation(testMethod);
        final var resource = yamlSource.value().length() == 0 ? testMethod.getName() + defaultExtension(jamal) : yamlSource.value();

        final Map<String, Map<String, Object>> parameters = getParameters(testClass, jamal, resource, yamlSource.ognl());

        return createArgumentsStream(testMethod, resource, parameters, yamlSource.strict());
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
                    setParameter(testMethod, strict, testYaml, displayName, para, i, names[i]);
                }
                list.add(Arguments.of(para));
                strictCheckExtra(testMethod, strict, nameSet, testYaml, displayName);
            }
        } catch (ClassCastException cce) {
            throw new ExtensionConfigurationException(format("The YAML source '%s' is not valid.", resourceName));
        }
        return list.stream();
    }

    private void setParameter(Method testMethod, boolean strict, Map.Entry<String, Map<String, Object>> testYaml, String displayName, Object[] para, int i, String name) {
        if ("DisplayName".equals(name)) {
            setDisplayName(displayName, para, i, testMethod.getParameters()[i].getType() == DisplayName.class);
        } else {
            strictCheckMissing(testMethod, strict, testYaml, displayName, name);
            para[i] = testYaml.getValue().get(name);
        }
    }

    private void strictCheckExtra(Method testMethod, boolean strict, Set<String> nameSet, Map.Entry<String, Map<String, Object>> testYaml, String displayName) {
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

    private void strictCheckMissing(Method testMethod, boolean strict, Map.Entry<String, Map<String, Object>> testYaml, String displayName, String name) {
        if (strict && !testYaml.getValue().containsKey(name)) {
            throw new ExtensionConfigurationException(
                format("The parameter '%s' in the test record '%s' of the test %s::%s() is not defined",
                    name,
                    displayName,
                    testMethod.getDeclaringClass().getName(), testMethod.getName()));
        }
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
     * @param testClass the class that the test method is in. This is used to identify the location of the resource. The
     *                  test data file is read from the same directory where the class is.
     * @param jamal     the Jamal annotation. It is used to get access to the Jamal parameters (e.g.: macro opening and
     *                  closing strings) as well as to know if Jamal processing is enabled.
     * @param resource  the name of the resource file that contains the Yaml/Jamal formatted parameters
     * @param ognl      the OGNL expression that selects where the test data starts
     * @return the Yaml structure read from the file
     * @throws URISyntaxException if the file cannot be identified
     */
    private Map<String, Map<String, Object>> getParameters(Class<?> testClass, Jamal jamal, String resource, String ognl) throws URISyntaxException {
        final StringBuilder sb = readResource(testClass, resource);
        try {
            final File file;
            if (resource.contains("\n")) {
                file = null;
            } else {
                file = Paths.get(requireNonNull(testClass.getResource(resource)).toURI()).toFile().getAbsoluteFile();
            }
            final String processed = processWithJamal(jamal, file, sb);
            final Map<String, Map<String, Object>> result =  yaml.load(processed);
            if( ognl.length() > 0 ){
                return (Map<String, Map<String, Object>>)Ognl.getValue(ognl,(Object)result,Map.class);
            }else{
                return result;
            }
        } catch (BadSyntax e) {
            throw new ExtensionConfigurationException(format("The source '%s' is not a valid Jamal source.", resource), e);
        } catch (ParserException e) {
            throw new ExtensionConfigurationException(format("The Yaml file '%s' is erroneous.", resource), e);
        } catch (OgnlException e) {
            throw new ExtensionConfigurationException(format("The Ognl file '%s' is erroneous.", ognl), e);
        }
    }

    /**
     * Reads the content of the resource.
     *
     * @param testClass the class that contains the test method. The resource will be read from the same
     *                  package/directory where the class file is.
     * @param resource  the local name of the resource. It is relative to the class name.
     * @return the content of the resource as a StringBuilder.
     */
    private StringBuilder readResource(Class<?> testClass, String resource) {
        if (resource.contains("\n")) {
            return new StringBuilder(resource);
        }
        final StringBuilder sb = new StringBuilder();
        try (final var is = testClass.getResourceAsStream(resource)) {
            if (is == null) {
                throw new ExtensionConfigurationException(format("The source '%s' is not found.", resource));
            }
            try (InputStreamReader isReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isReader)) {
                String str;
                while ((str = reader.readLine()) != null) {
                    sb.append(str).append("\n");
                }
            }
        } catch (IOException ioe) {
            throw new ExtensionConfigurationException(format("The source '%s' is not readable.", resource), ioe);
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
        final var path = file == null ? "." : file.getPath();
        final String processed;
        if (jamal.enabled()) {
            try (final var processor = new Processor(jamal.open(), jamal.close())) {
                processed = processor.process(Input.makeInput(sb.toString(), new Position(path)));
                createDumpFile(jamal, file, processed);
            }
        } else {
            processed = sb.toString();
        }
        return processed;
    }

    /**
     * Print the result of the Jamal processing into an utput file in case there is a Jamal annotation with the
     * parameter dump, that specifies the output file. This file is not used by the application. It is there only for
     * debug purposes in case you want to see what Yaml was created from the Jamal file.
     *
     * @param jamal     is the Jamal annotation
     * @param file      the input file or null in case the input comes from the annotation string
     * @param processed the Jamal processed output
     */
    private void createDumpFile(Jamal jamal, File file, String processed) {
        if (jamal.dump().length() > 0) {
            final File out = getDumpFile(jamal.dump(), file);
            try (final var fos = new FileOutputStream(out)) {
                fos.write(processed.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new ExtensionConfigurationException(format("Cannot write the dump file '%s'.", jamal.dump()), e);
            }
        }
    }

    /**
     * Get the dump file for the debug output. If the input comes from a file then the output fill be in the same
     * directory as named by the annotation Jamal. If the input comes from the annotation string then the output fill go
     * to the current working directory.
     *
     * @param dump the file name
     * @param file the file to the input of null in case the input comes directly from the string given on the
     *             annotation
     * @return the output File object
     */
    private File getDumpFile(String dump, File file) {
        final File out;
        if (file == null) {
            out = new File(dump);
        } else {
            out = new File(file.getParentFile(), dump);
        }
        return out;
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
     * @param testMethod the test method, which may be annotated with @{@link Jamal}
     * @return a @{@link Jamal} annotation instance (may be the the default if not specified anywhere).
     */
    private Jamal getJamalAnnotation(java.lang.reflect.Method testMethod) {
        final var annotations = new ArrayList<Jamal>();
        addJamalAnnotation(testMethod, annotations);
        var klass = testMethod.getDeclaringClass();
        while (klass != null) {
            addJamalAnnotation(klass, annotations);
            klass = klass.getDeclaringClass();
        }
        final var jamal = new Jamal.Collected();
        for (int i = annotations.size() - 1; i >= 0; i--) {
            final var inheritFrom = annotations.get(i);
            if (inheritFrom.open().length() != 0) {
                jamal.open = inheritFrom.open();
            }
            if (inheritFrom.close().length() != 0) {
                jamal.close = inheritFrom.close();
            }
            if (inheritFrom.dump().length() != 0) {
                jamal.dump = inheritFrom.dump();
            }
            jamal.enabled = inheritFrom.enabled();
        }
        if (jamal.open == null || jamal.open().equals("")) {
            jamal.open = "{%";
        }
        if (jamal.close == null || jamal.close().equals("")) {
            jamal.close = "%}";
        }
        return jamal;
    }

    private void addJamalAnnotation(AnnotatedElement testMethod, List<Jamal> annotations) {
        final var jamalOnMethod = findAnnotation(testMethod, Jamal.class);
        final var jamalOnMethodAnnotation = findAnnotation(testMethod, YamlSource.class).map(YamlSource::jamal).filter(j -> !j.open().equals("\u0000"));
        if (jamalOnMethod.isPresent() && jamalOnMethodAnnotation.isPresent()) {
            throw new IllegalArgumentException("You cannot have @Jamal annotation on a test method or class and in the @YamlSource as well.");
        }
        jamalOnMethod.ifPresent(annotations::add);
        jamalOnMethodAnnotation.ifPresent(annotations::add);
    }

    /**
     * This argument provider can only be used with test methods, which are annotated with {@link YamlSource}. The
     * annotation can also be specified on the declaring class. In this case the string (value and ognl) values that are
     * defined on the class but not on the method are inherited from the annotation on the class. The inheritance walks
     * through the whole inner cluss structure in case the method is in an inner class. In that case the annotation
     * parameters are inherited from the inner classes as well as from the top level class if there is any.
     *
     * @param testMethod the test method
     * @return the annotation object that controls the execution of this run of this argument provider
     */
    private YamlSource getYamlSourceAnnotation(Method testMethod) {
        final var annotations = new ArrayList<YamlSource>();
        final var yamlSourceOnMethod = findAnnotation(testMethod, YamlSource.class)
            .orElseThrow(() -> new ExtensionConfigurationException(format("Test method is not annotated with '@%s'",
                YamlSource.class.getSimpleName())));
        annotations.add(yamlSourceOnMethod);
        var klass = testMethod.getDeclaringClass();
        while (klass != null) {
            findAnnotation(klass, YamlSource.class).ifPresent(annotations::add);
            klass = klass.getDeclaringClass();
        }
        final var yamlSource = new YamlSource.Collected();
        for (int i = annotations.size() - 1; i >= 0; i--) {
            final var inheritFrom = annotations.get(i);
            if (inheritFrom.ognl().length() != 0) {
                yamlSource.ognl = inheritFrom.ognl();
            }
            if (inheritFrom.value().length() != 0) {
                yamlSource.value = inheritFrom.value();
            }
            yamlSource.strict = inheritFrom.strict();
        }
        return yamlSource;
    }


}
