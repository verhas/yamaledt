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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class YamalArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        final var testMethod = extensionContext.getRequiredTestMethod();
        final var testClass = testMethod.getDeclaringClass();
        final YamlSource yamlSource = getYamlSourceAnnotation(testMethod, testClass);
        final Jamal jamal = getJamalAnnotation(testMethod, testClass);

        final var resourceName = yamlSource.value().length() == 0 ? testMethod.getName() + defaultExtension(jamal) : yamlSource.value();

        final Object parameters;
        try (final var is = testClass.getResourceAsStream(resourceName)) {
            final var file = Paths.get(testClass.getResource(resourceName).toURI()).toFile().getAbsoluteFile();
            final var path = file.getPath();
            final var sb = readStream(is);
            final String processed;
            if (jamal.enabled()) {
                final var processor = new Processor(jamal.open(), jamal.close());
                processed = processor.process(Input.makeInput(sb.toString(), new Position(path)));
                if( jamal.dump().length() > 0 ){
                    final var out = new File(file.getParentFile(),jamal.dump());
                    try(final var fos = new FileOutputStream(out)){
                        fos.write(processed.getBytes(StandardCharsets.UTF_8));
                    }
                }
            } else {
                processed = sb.toString();
            }
            Yaml yaml = new Yaml();
            parameters = yaml.load(processed);
        } catch (IOException ioe) {
            throw new ExtensionConfigurationException(format("The source '%s' is not readable.", resourceName));
        } catch (BadSyntax e) {
            throw new ExtensionConfigurationException(format("The source '%s' is not a valid Jamal source.", resourceName), e);
        }


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

    private String defaultExtension(Jamal jamal) {
        return jamal.enabled() ? ".yaml.jam" : ".yaml";
    }

    private Jamal getJamalAnnotation(java.lang.reflect.Method testMethod, Class<?> testClass) {
        final var jamal = findAnnotation(testMethod, Jamal.class)
            .orElseGet(() -> findAnnotation(testClass, Jamal.class).orElseGet(() -> new JamalDefault()));
        return jamal;
    }

    private YamlSource getYamlSourceAnnotation(java.lang.reflect.Method testMethod, Class<?> testClass) {
        final var yamlSource = findAnnotation(testMethod, YamlSource.class)
            .orElseGet(() -> findAnnotation(testClass, YamlSource.class)
                .orElseThrow(() -> new ExtensionConfigurationException(format("Test method is not annotated with '@%s'",
                    YamlSource.class.getSimpleName()))));
        return yamlSource;
    }


    private StringBuffer readStream(InputStream is) throws IOException {
        final var sb = new StringBuffer();
        try (InputStreamReader isReader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isReader)) {
            String str;
            while ((str = reader.readLine()) != null) {
                sb.append(str).append("\n");
            }
        }
        return sb;
    }
}
