package javax0.yamaledt.utils;

import javax0.yamaledt.Jamal;

import java.lang.annotation.Annotation;

public class JamalDefault implements Jamal {

    @Override
    public String open() {
        return "{%";
    }

    @Override
    public String close() {
        return "%}";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String dump() {
        return "";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Jamal.class;
    }
}
