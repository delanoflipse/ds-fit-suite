package io.github.delanoflipse.fit.suite.strategy.components;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.delanoflipse.fit.suite.faultload.Behaviour;
import io.github.delanoflipse.fit.suite.faultload.Fault;
import io.github.delanoflipse.fit.suite.faultload.FaultUid;
import io.github.delanoflipse.fit.suite.faultload.modes.FailureMode;
import io.github.delanoflipse.fit.suite.strategy.util.Pair;
import io.github.delanoflipse.fit.suite.trace.tree.TraceReport;

public abstract class PruneContext {
    public abstract List<Pair<Set<Fault>, List<Behaviour>>> getHistoricResults();

    public abstract List<FailureMode> getFailureModes();

    public abstract List<FaultUid> getFaultInjectionPoints();

    public abstract Set<Behaviour> getExpectedBehaviours(Set<Fault> faultload);

    public abstract Set<FaultUid> getExpectedPoints(Set<Fault> faultload);

    public abstract Map<FaultUid, TraceReport> getHappyPath();

    public TraceReport getHappyPath(FaultUid uid) {
        return getHappyPath().get(uid);
    }

    public List<TraceReport> getHappyPaths() {
        return getHappyPath().values().stream().toList();
    }

    public abstract long spaceSize();
}
