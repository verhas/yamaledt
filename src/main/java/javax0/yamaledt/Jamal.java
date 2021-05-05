package javax0.yamaledt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Jamal {
    String open() default "{%";

    String close() default "%}";

    boolean enabled() default true;

    String dump() default "";
}
