package javax0.yamaledt;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ArgumentsSource(YamalArgumentsProvider.class)
public @interface YamlSource {
    class Collected implements YamlSource {
        String value = "";
        String ognl = "";
        boolean strict = false;
        Jamal jamal;

        @Override
        public String value() {
            return value;
        }

        @Override
        public String ognl() {
            return ognl;
        }

        @Override
        public Jamal jamal() {
            return jamal;
        }

        @Override
        public boolean strict() {
            return strict;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return YamlSource.class;
        }
    }

    /**
     * @return the name of the yaml resource that contains the test data. If it is not defined then the application will
     * use the name of the test method and appends the '.yaml' or '.yaml.jam' extension
     */
    String value() default "";

    /**
     * @return The OGNL expression where the test data starts.
     */
    String ognl() default "";

    /**
     * @return the jamal annotation. This is here as a parameter to support those developers who do not read the
     * documentation and find out this possibility using the automcomplete feature of the IDE. Otherwise use this
     * annotation on the method and not here as an annotation parameter.
     */
    Jamal jamal() default @Jamal(enabled = true, open = "\u0000");

    /**
     * Setting this {@code true} will throw error if some test parameter isnot defined (default behaviour in that case
     * is to use null) or there is a parameter in the Yaml file, which is not used by the test method. The default
     * behaviour is to be lenient.
     *
     * @return false by default
     */
    boolean strict() default false;
}
