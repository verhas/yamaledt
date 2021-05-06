package javax0.yamaledt;

import javax0.yamaledt.utils.JamalDefault;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ArgumentsSource(YamalArgumentsProvider.class)
public @interface YamlSource {
    /**
     * @return the name of the yaml resource that contains the test data
     */
    String value() default "";

    Jamal jamal() default @Jamal(enabled = true, open = "\u0000");
}
