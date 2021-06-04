module yamaledt {
    requires transitive org.junit.platform.commons;
    requires transitive org.junit.jupiter.params;
    requires transitive org.junit.jupiter.api;
    requires transitive org.junit.jupiter.engine;
    requires jamal.engine;
    requires jamal.tools;
    requires jamal.api;
    requires org.yaml.snakeyaml;
    requires ognl;
    exports javax0.yamaledt;
}