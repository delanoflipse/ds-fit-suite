package nl.dflipse.fit;

import java.io.IOException;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.instrument.FaultController;
import nl.dflipse.fit.instrument.InstrumentedApp;
import nl.dflipse.fit.instrument.services.InstrumentedService;

/**
 * FI test the app
 */
@SuppressWarnings("resource")
@Testcontainers(parallel = true)
public class AppTest {
    public static final InstrumentedApp app = new InstrumentedApp().withJaeger();
    private static final String BASE_IMAGE = "go-micro-service:latest";

    @Container
    private static final InstrumentedService geo = app.instrument("geo", 8080,
            new GenericContainer<>(BASE_IMAGE)
                    .withCommand("go-micro-services geo"))
            .withHttp2();

    @Container
    private static final InstrumentedService rate = app.instrument("rate", 8080,
            new GenericContainer<>(BASE_IMAGE)
                    .withCommand("go-micro-services rate"))
            .withHttp2();

    @Container
    private static final InstrumentedService search = app.instrument("search", 8080,
            new GenericContainer<>(BASE_IMAGE)
                    .withCommand("go-micro-services search"))
            .withHttp2();

    @Container
    private static final InstrumentedService profile = app.instrument("profile", 8080,
            new GenericContainer<>(BASE_IMAGE)
                    .withCommand("go-micro-services profile"))
            .withHttp2();

    @Container
    private static final GenericContainer<?> frontend = new GenericContainer<>(BASE_IMAGE)
            .withNetwork(app.network)
            .withCommand("go-micro-services frontend")
            .withExposedPorts(8080)
            .dependsOn(search.getService(), profile.getService());

    public static FaultController getController() {
        return app;
    }

    @BeforeAll
    public static void setupServices() {
        app.start();
    }

    @AfterAll
    static public void teardownServices() {
        app.stop();
    }

    @FiTest
    public void testApp(Faultload faultload) throws IOException {
        int frontendPort = frontend.getMappedPort(8080);
        String queryUrl = "http://localhost:" + frontendPort + "/hotels?inDate=2015-04-09&outDate=2015-04-10";

        Response res = Request.get(queryUrl)
                .addHeader("traceparent", faultload.getTraceParent().toString())
                .addHeader("tracestate", faultload.getTraceState().toString())
                .execute();

        String inspectUrl = app.orchestratorInspectUrl + "/v1/get/" + faultload.getTraceId();
        String traceUrl = "http://localhost:" + app.jaeger.getMappedPort(app.jaegerPort) + "/trace/"
                + faultload.getTraceId();

        boolean containsError = faultload.getFaults().stream()
                .anyMatch(f -> f.getMode().getType().equals("HTTP_ERROR"));
        int expectedResponse = containsError ? 500 : 200;
        int actualResponse = res.returnResponse().getCode();
        assertEquals(expectedResponse, actualResponse);

        boolean allRunning = app.allRunning();
        assertTrue(allRunning);
    }
}
