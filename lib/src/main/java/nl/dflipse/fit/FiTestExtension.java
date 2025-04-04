package nl.dflipse.fit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.dflipse.fit.faultload.faultmodes.ErrorFault;
import nl.dflipse.fit.faultload.faultmodes.HttpError;
import nl.dflipse.fit.instrument.FaultController;
import nl.dflipse.fit.strategy.FaultloadResult;
import nl.dflipse.fit.strategy.StrategyRunner;
import nl.dflipse.fit.strategy.TrackedFaultload;
import nl.dflipse.fit.strategy.analyzers.BreadthFirstDetector;
import nl.dflipse.fit.strategy.analyzers.ConcurrencyDetector;
import nl.dflipse.fit.strategy.analyzers.ConditionalFaultDetector;
import nl.dflipse.fit.strategy.analyzers.RedundancyAnalyzer;
import nl.dflipse.fit.strategy.analyzers.StatusAnalyzer;
import nl.dflipse.fit.strategy.generators.IncreasingSizeGenerator;
import nl.dflipse.fit.strategy.pruners.CauseEffectPruner;
import nl.dflipse.fit.strategy.pruners.DynamicReductionPruner;
import nl.dflipse.fit.strategy.pruners.ErrorPropogationPruner;
import nl.dflipse.fit.strategy.pruners.FailStopPruner;
import nl.dflipse.fit.strategy.pruners.FaultloadSizePruner;
import nl.dflipse.fit.strategy.pruners.NoImpactPruner;
import nl.dflipse.fit.strategy.pruners.ParentChildPruner;
import nl.dflipse.fit.strategy.util.TraceAnalysis;

public class FiTestExtension
        implements TestTemplateInvocationContextProvider {
    private StrategyRunner strategy;
    private static final Logger logger = LoggerFactory.getLogger(FiTestExtension.class);

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        // Check if the annotation is present
        return context.getTestMethod().isPresent() &&
                context.getTestMethod().get().isAnnotationPresent(FiTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        // Retrieve the annotation and its parameters
        var annotation = context.getTestMethod()
                .orElseThrow()
                .getAnnotation(FiTest.class);

        if (annotation == null) {
            annotation = context.getRequiredTestClass().getAnnotation(FiTest.class);
        }

        var modes = List.of(
                ErrorFault.fromError(HttpError.SERVICE_UNAVAILABLE),
                ErrorFault.fromError(HttpError.BAD_GATEWAY),
                ErrorFault.fromError(HttpError.INTERNAL_SERVER_ERROR),
                ErrorFault.fromError(HttpError.GATEWAY_TIMEOUT));

        boolean onlyPersistantOrTransientRetries = annotation.optimizeForRetries();
        boolean pruneImpactless = annotation.optimizeForImpactless();
        int maxFaultloadSize = annotation.maxFaultloadSize();

        strategy = new StrategyRunner()
                .withComponent(new IncreasingSizeGenerator(modes))
                // .withComponent(new RandomDetector())
                .withComponent(new BreadthFirstDetector())
                // .withComponent(new DepthFirstDetector())
                .withComponent(new ConditionalFaultDetector(onlyPersistantOrTransientRetries))
                .withComponent(new RedundancyAnalyzer())
                .withComponent(new StatusAnalyzer())
                .withComponent(new ConcurrencyDetector())
                .withComponent(new ParentChildPruner())
                .withComponent(new ErrorPropogationPruner())
                .withComponent(new CauseEffectPruner())
                .withComponent(new NoImpactPruner(pruneImpactless))
                .withComponent(new DynamicReductionPruner());

        if (annotation.maxTestCases() > 0) {
            strategy.withMaxTestCases(annotation.maxTestCases());
        }

        if (maxFaultloadSize > 0) {
            strategy.withComponent(new FaultloadSizePruner(maxFaultloadSize));
        }

        if (annotation.failStop()) {
            strategy.withComponent(new FailStopPruner());
        }

        if (annotation.maskPayload()) {
            strategy.withPayloadMasking();
        }

        if (annotation.hashBody()) {
            strategy.withBodyHashing();
        }

        if (annotation.logHeaders()) {
            strategy.withLogHeader();
        }

        if (annotation.getTraceInitialDelay() > 0) {
            strategy.withGetDelay(annotation.getTraceInitialDelay());
        }

        Class<?> testClass = context.getRequiredTestClass();
        FaultController controller;

        try {
            controller = (FaultController) testClass.getMethod("getController").invoke(null);
            if (controller == null) {
                throw new IllegalStateException(
                        "Test has no public static field getController(). Ensure getController() exists and returns a faultcontroller.");
            }
        } catch (NoSuchMethodException | NullPointerException | IllegalArgumentException | IllegalAccessException
                | InvocationTargetException | SecurityException e) {
            throw new RuntimeException("Failed to access getControleler from test class", e);
        }

        return Stream
                .generate(() -> createInvocationContext(strategy, controller))
                .takeWhile(ctx -> ctx != null)
                .onClose(() -> {
                    afterAll();
                });
    }

    private TestTemplateInvocationContext createInvocationContext(StrategyRunner strategy, FaultController controller) {
        TrackedFaultload faultload = strategy.nextFaultload();

        if (faultload == null) {
            return null;
        }

        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return "[" + invocationIndex + "] TraceId=" + faultload.getTraceId() + " Faultload="
                        + faultload.getFaultload().readableString();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(
                        new QueueParameterResolver(faultload),
                        new BeforeTestExtension(faultload, controller),
                        new AfterTestExtension(faultload, strategy, controller));
            }
        };
    }

    public void afterAll() {
        strategy.statistics.setSize(strategy.getGenerator().spaceSize());
        strategy.statistics.report();
    }

    // Parameter resolver to inject the current parameter into the test
    private static class QueueParameterResolver implements ParameterResolver {
        private final TrackedFaultload faultload;

        QueueParameterResolver(TrackedFaultload faultload) {
            this.faultload = faultload;
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            return parameterContext.getParameter().getType().equals(TrackedFaultload.class);
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            return faultload;
        }
    }

    // Before each test, register the faultload with the proxies
    private static class BeforeTestExtension implements BeforeTestExecutionCallback {
        private final TrackedFaultload faultload;
        private final FaultController controller;

        BeforeTestExtension(TrackedFaultload faultload, FaultController controller) {
            this.faultload = faultload;
            this.controller = controller;
        }

        @Override
        public void beforeTestExecution(ExtensionContext context) {
            if (faultload == null) {
                return;
            }

            String displayName = context.getDisplayName();
            System.out.println();
            logger.info("Test " + displayName);

            faultload.timer.start();
            faultload.timer.start("registerFaultload");
            try {
                controller.registerFaultload(faultload);
            } catch (IOException e) {
                throw new RuntimeException("Failed to register faultload", e);
            }
            faultload.timer.stop("registerFaultload");
            faultload.timer.start("testMethod");
        }
    }

    // After each test, handle the result and update the strategy
    private static class AfterTestExtension implements AfterTestExecutionCallback {
        private final TrackedFaultload faultload;
        private final StrategyRunner strategy;
        private final FaultController controller;

        AfterTestExtension(TrackedFaultload faultload, StrategyRunner strategy, FaultController controller) {
            this.faultload = faultload;
            this.strategy = strategy;
            this.controller = controller;
        }

        @Override
        public void afterTestExecution(ExtensionContext context) {
            faultload.timer.stop("testMethod");
            // Access the queue and test result
            // var testMethod = context.getTestMethod().orElseThrow();
            // var annotation = testMethod.getAnnotation(FiTest.class);

            boolean testFailed = context.getExecutionException().isPresent();
            logger.info("Invariant: " + (testFailed ? "VIOLATED" : "HOLDS"));

            strategy.statistics.registerRun();

            try {
                TraceAnalysis trace = controller.getTrace(faultload);
                int status = trace.getRootReport().response.status;
                logger.info("Client responded with HTTP status: {}", status);
                faultload.timer.start("handleResult");
                FaultloadResult result = new FaultloadResult(faultload, trace, !testFailed);
                strategy.handleResult(result);
                faultload.timer.stop("handleResult");
            } catch (IOException e) {
                e.printStackTrace();
            }

            faultload.timer.start("unregisterFautload");
            try {
                controller.unregisterFaultload(faultload);
            } catch (Exception e) {
                e.printStackTrace();
            }
            faultload.timer.stop("unregisterFautload");

            faultload.timer.stop();
            strategy.registerTime(faultload);
        }
    }

}
