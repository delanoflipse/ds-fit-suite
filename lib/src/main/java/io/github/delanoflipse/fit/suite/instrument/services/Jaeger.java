package io.github.delanoflipse.fit.suite.instrument.services;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

public class Jaeger extends GenericContainer<Jaeger> {
    private static String defaultImage = "jaegertracing/jaeger:latest";

    public Jaeger(String imageName) {
        super(imageName);
        withEnv(Map.of(
                "COLLECTOR_OTLP_ENABLED", "true"));
    }

    public Jaeger() {
        this(defaultImage);
    }
}
