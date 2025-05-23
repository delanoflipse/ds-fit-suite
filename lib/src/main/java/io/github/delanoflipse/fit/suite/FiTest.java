package io.github.delanoflipse.fit.suite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import io.github.delanoflipse.fit.suite.strategy.util.TraceAnalysis.TraversalStrategy;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.METHOD) // Apply to methods
@Retention(RetentionPolicy.RUNTIME) // Retain at runtime so JUnit can read it
@TestTemplate // Mark this as a template for multiple executions
@ExtendWith(FiTestExtension.class) // Link to the extension
public @interface FiTest {
    boolean maskPayload() default false;

    boolean hashBody() default false;

    boolean logHeaders() default false;

    boolean withCallStack() default false;

    boolean failStop() default true;

    boolean optimizeForRetries() default false;

    boolean optimizeForImpactless() default false;

    long maxTestCases() default 0;

    long maxTimeS() default 0;

    int maxFaultloadSize() default 0;

    int initialGetTraceDelay() default 0;

    Class<?>[] additionalComponents() default {};

    TraversalStrategy traversalStrategy() default TraversalStrategy.DEPTH_FIRST;
}
