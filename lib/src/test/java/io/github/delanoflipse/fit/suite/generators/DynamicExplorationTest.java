package io.github.delanoflipse.fit.suite.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.Test;

import io.github.delanoflipse.fit.suite.faultload.Behaviour;
import io.github.delanoflipse.fit.suite.faultload.Faultload;
import io.github.delanoflipse.fit.suite.strategy.FaultloadResult;
import io.github.delanoflipse.fit.suite.strategy.TrackedFaultload;
import io.github.delanoflipse.fit.suite.strategy.components.PruneDecision;
import io.github.delanoflipse.fit.suite.strategy.components.generators.DynamicExplorationGenerator;
import io.github.delanoflipse.fit.suite.strategy.store.ImplicationsStore;
import io.github.delanoflipse.fit.suite.strategy.util.TraceAnalysis;
import io.github.delanoflipse.fit.suite.testutil.EventBuilder;
import io.github.delanoflipse.fit.suite.testutil.FailureModes;
import io.github.delanoflipse.fit.suite.trace.tree.TraceReport;
import io.github.delanoflipse.fit.suite.trace.tree.TraceResponse;

public class DynamicExplorationTest {

    private FaultloadResult toResult(Faultload f, ImplicationsStore store) {
        var root = store.getRootCause();
        List<TraceReport> reports = new ArrayList<>();

        for (var behav : store.getBehaviours(f.faultSet())) {
            TraceReport report = asReport(behav, f);
            if (behav.uid().matches(root)) {
                report.isInitial = true;
            }
            reports.add(report);
        }

        TraceAnalysis trace = new TraceAnalysis(reports);
        return new FaultloadResult(new TrackedFaultload(f), trace, true);
    }

    private TraceReport asReport(Behaviour behaviour, Faultload f) {
        TraceReport report = new TraceReport();
        report.traceId = "";
        report.spanId = "";
        report.faultUid = behaviour.uid();
        report.concurrentTo = List.of();
        report.injectedFault = f.faultSet().stream()
                .filter(x -> x.uid().matches(behaviour.uid()))
                .findFirst()
                .orElse(null);
        int code = behaviour.mode() == null ? 200 : Integer.parseInt(behaviour.mode().args().get(0));

        TraceResponse response = new TraceResponse();
        response.status = code;
        response.body = "";
        response.durationMs = 1;
        report.response = response;
        return report;
    }

    private List<Faultload> playout(DynamicExplorationGenerator generator, ImplicationsStore store) {
        Faultload base = new Faultload(Set.of());
        FaultloadResult result = toResult(base, store);
        generator.handleFeedback(result, generator);

        List<Faultload> visited = new ArrayList<>();
        visited.add(base);

        while (true) {
            var next = generator.generate();
            if (next == null) {
                break;
            }

            visited.add(next);
            FaultloadResult nextResult = toResult(next, store);
            generator.handleFeedback(nextResult, generator);
        }

        return visited;
    }

    @Test
    public void testHappyPathOnly() {
        var modes = FailureModes.getModes(1);

        var a = new EventBuilder("A");
        var b = a.createChild("B");
        var c = a.createChild("C");
        var d = c.createChild("D");

        ImplicationsStore store = new ImplicationsStore();
        store.addDownstreamRequests(a.uid(), List.of(b.uid(), c.uid()));
        store.addDownstreamRequests(c.uid(), List.of(d.uid()));

        DynamicExplorationGenerator generator = new DynamicExplorationGenerator(modes, x -> PruneDecision.KEEP);
        var result = playout(generator, store);

        // We explore the full space, because the generator visits the redundant
        // cases too. ({B,D} -> we can reach C!)
        assertEquals(8, result.size());
    }

    @Test
    public void testWithExclusion() {
        var modes = FailureModes.getModes(1);

        var a = new EventBuilder("A");
        var b = a.createChild("B");
        var c = a.createChild("C");
        var d = c.createChild("D");

        ImplicationsStore store = new ImplicationsStore();
        store.addDownstreamRequests(a.uid(), List.of(b.uid(), c.uid()));
        store.addDownstreamRequests(c.uid(), List.of(d.uid()));
        // B excludes C
        store.addExclusionEffect(Set.of(b.behaviour().asMode(modes.get(0))), c.uid());

        DynamicExplorationGenerator generator = new DynamicExplorationGenerator(modes, x -> PruneDecision.KEEP);
        var result = playout(generator, store);

        // We omit visiting B,C,D
        // because at C,D we only inject C
        assertEquals(7, result.size());
    }

    @Test
    public void testWithInclusion() {
        var modes = FailureModes.getModes(1);

        var a = new EventBuilder("A");
        var b = a.createChild("B");
        var c = a.createChild("C");
        var d = c.createChild("D");
        var e = c.createChild("E");

        ImplicationsStore store = new ImplicationsStore();
        store.addDownstreamRequests(a.uid(), List.of(b.uid(), c.uid()));
        store.addDownstreamRequests(c.uid(), List.of(d.uid()));
        // D includes E
        store.addInclusionEffect(Set.of(d.behaviour().asMode(modes.get(0))), e.uid());

        DynamicExplorationGenerator generator = new DynamicExplorationGenerator(modes, x -> PruneDecision.KEEP);
        var result = playout(generator, store);

        // We visit the full B,C,D space => 2^3=8
        // And all {D,E} nodes 2^2=4
        assertEquals(12, result.size());
    }

    @Test
    public void testWithExclusionAndInclusion() {
        var modes = FailureModes.getModes(1);

        var a = new EventBuilder("A");
        var b = a.createChild("B");
        var c = a.createChild("C");
        var d = c.createChild("D");
        var e = c.createChild("E");

        ImplicationsStore store = new ImplicationsStore();
        store.addDownstreamRequests(a.uid(), List.of(b.uid(), c.uid()));
        store.addDownstreamRequests(c.uid(), List.of(d.uid()));
        // B excludes C
        store.addExclusionEffect(Set.of(b.behaviour().asMode(modes.get(0))), c.uid());
        // D includes E
        store.addInclusionEffect(Set.of(d.behaviour().asMode(modes.get(0))), e.uid());

        DynamicExplorationGenerator generator = new DynamicExplorationGenerator(modes, x -> PruneDecision.KEEP);
        var result = playout(generator, store);

        // We omit B,C,D,E and B,C,D
        assertEquals(10, result.size());
    }
}
