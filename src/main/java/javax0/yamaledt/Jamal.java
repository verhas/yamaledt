package javax0.yamaledt;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Jamal {
    class Collected implements Jamal {
        String open = "";
        String close = "";
        boolean enabled = true;
        String dump = "";

        @Override
        public String open() {
            return open;
        }

        @Override
        public String close() {
            return close;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public String dump() {
            return dump;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Jamal.class;
        }
    }

    /**
     * The macro opening string. The usual opening brace is not really good, since Yaml also uses that character to
     * start JSON formatted maps.
     *
     * @return
     */
    String open() default "";

    /**
     * The macro closing string. The usual closing brace is not really good, since Yaml also uses that character to for
     * JSON formatted maps.
     *
     * @return
     */
    String close() default "";

    /**
     * This parameter can be used to disable the Jamal processing
     *
     * @return
     */
    boolean enabled() default true;

    /**
     * Thi sparameter can define a file name to be used to dump the output of Jama processing. This can be useful when
     * the Yaml processing does not work for some reason. The Jamal processing can also be debugged specifying the
     * environment variable {@code JAMAL_DEBUG=http:8080}, but here it is another possibility.
     *
     * @return
     */
    String dump() default "";
}
