package nl.dflipse.fit.instrument.services;

import java.util.List;

import org.testcontainers.containers.GenericContainer;

import nl.dflipse.fit.instrument.InstrumentedApp;

public class InstrumentedService extends GenericContainer<InstrumentedService> {
    private static final String IMAGE_NAME = "fit-proxy:latest";
    private final GenericContainer<?> service;
    private final String hostname;
    private final String serviceHostname;
    private final int port;
    private final int controlPort;

    public InstrumentedService(GenericContainer<?> service, String hostname, int port, InstrumentedApp app) {
        super(IMAGE_NAME);

        this.hostname = hostname;
        this.serviceHostname = hostname + "-instrumented";
        this.port = port;
        this.controlPort = port + 1;
        this.service = service;

        this.dependsOn(service)
                .dependsOn(service)
                .withEnv("PROXY_HOST", "0.0.0.0:" + port)
                .withEnv("PROXY_TARGET", "http://" + this.serviceHostname + ":" + port)
                .withEnv("ORCHESTRATOR_HOST", app.orchestratorHost + ":" + app.orchestratorPort)
                .withEnv("SERVICE_NAME", hostname)
                .withNetwork(app.network)
                .withNetworkAliases(hostname);

        service.setNetwork(app.network);
        service.setNetworkAliases(List.of(this.serviceHostname));
    }

    public InstrumentedService withHttp2() {
        withEnv("USE_HTTP2", "true");
        return this;
    }

    public String getControlHost() {
        // controller port is original port + 1
        return hostname + ":" + this.controlPort;
    }

    public GenericContainer<?> getService() {
        return service;
    }

    public int getPort() {
        return port;
    }

    @Override
    public void start() {
        service.start();
        super.start();
    }

    @Override
    public void stop() {
        service.stop();
        super.stop();
    }

}