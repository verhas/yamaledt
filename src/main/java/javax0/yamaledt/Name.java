package javax0.yamaledt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Name {
    /**
     * This annotatiom can be used to specify the name of the parameter. This name will be used as a key to fetch the
     * value from the Yaml test data structure. If this annotation is not used on a parameter then the name of the
     * parameter type will be used as key.
     *
     * @return
     */
    String value();
}
