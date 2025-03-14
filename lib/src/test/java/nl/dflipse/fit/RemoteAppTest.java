package nl.dflipse.fit;

import java.io.IOException;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import static org.junit.Assert.assertEquals;

import nl.dflipse.fit.faultload.faultmodes.ErrorFault;
import nl.dflipse.fit.faultload.faultmodes.OmissionFault;
import nl.dflipse.fit.instrument.FaultController;
import nl.dflipse.fit.instrument.controller.RemoteController;
import nl.dflipse.fit.strategy.TrackedFaultload;

/**
 * FI test the app
 */
public class RemoteAppTest {
    public static final RemoteController controller = new RemoteController("http://localhost:5000");

    public static FaultController getController() {
        return controller;
    }

    @FiTest
    public void testApp(TrackedFaultload faultload) throws IOException {
        int frontendPort = 8080;
        String queryUrl = "http://localhost:" + frontendPort + "/hotels?inDate=2015-04-09&outDate=2015-04-10";

        Response res = Request.get(queryUrl)
                .addHeader("traceparent", faultload.getTraceParent().toString())
                .addHeader("tracestate", faultload.getTraceState().toString())
                .execute();

        String inspectUrl = "http://localhost:5000/v1/trace/" + faultload.getTraceId();
        String traceUrl = "http://localhost:16686/trace/"
                + faultload.getTraceId();

        boolean containsError = faultload.hasFaultMode(ErrorFault.FAULT_TYPE, OmissionFault.FAULT_TYPE);
        int expectedResponse = containsError ? 500 : 200;
        int actualResponse = res.returnResponse().getCode();
        assertEquals(expectedResponse, actualResponse);

    }
}
