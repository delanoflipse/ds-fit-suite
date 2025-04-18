package nl.dflipse.fit.strategy.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.dflipse.fit.faultload.Behaviour;
import nl.dflipse.fit.faultload.Fault;
import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.faultload.modes.FailureMode;
import nl.dflipse.fit.strategy.util.Sets;
import nl.dflipse.fit.strategy.util.SpaceEstimate;
import nl.dflipse.fit.util.NoOpLogger;

public class DynamicAnalysisStore {
    private final Logger logger;
    private final List<FailureMode> modes;
    private final List<FaultUid> points = new ArrayList<>();

    private final ImplicationsStore implicationsStore = new ImplicationsStore();

    private final List<Set<Fault>> redundantFaultloads = new ArrayList<>();
    private final List<Set<FaultUid>> redundantUidSubsets = new ArrayList<>();
    private final List<Set<Fault>> redundantFaultSubsets = new ArrayList<>();

    public DynamicAnalysisStore(List<FailureMode> modes, boolean quiet) {
        this.modes = modes;
        if (quiet) {
            logger = new NoOpLogger();
        } else {
            logger = LoggerFactory.getLogger(DynamicAnalysisStore.class);
        }
    }

    public DynamicAnalysisStore(List<FailureMode> modes) {
        this(modes, false);
    }

    public List<FailureMode> getModes() {
        return modes;
    }

    public List<FaultUid> getPoints() {
        return points;
    }

    public List<Set<Fault>> getRedundantFaultloads() {
        return this.redundantFaultloads;
    }

    public List<Set<FaultUid>> getRedundantUidSubsets() {
        return this.redundantUidSubsets;
    }

    public List<Set<Fault>> getRedundantFaultSubsets() {
        return this.redundantFaultSubsets;
    }

    public Map<String, String> getImplicationsReport() {
        return implicationsStore.getReport();
    }

    public Set<FaultUid> getNonConditionalFaultUids() {
        return points.stream()
                .filter(fid -> implicationsStore.isInclusionEffect(fid))
                .collect(Collectors.toSet());
    }

    public boolean hasFaultUid(FaultUid fid) {
        return points.contains(fid);
    }

    public boolean addFaultUid(FaultUid fid) {
        if (hasFaultUid(fid)) {
            return false;
        }

        points.add(fid);
        return true;
    }

    public boolean addUpstreamEffect(FaultUid fid, Collection<FaultUid> children) {
        return implicationsStore.addUpstreamEffect(fid, children);
    }

    public boolean addDownstreamEffect(Collection<Behaviour> condition, Behaviour effect) {
        return implicationsStore.addDownstreamEffect(condition, effect);
    }

    public boolean addConditionForFaultUid(Collection<Behaviour> condition, FaultUid inclusion) {
        boolean isNew = addFaultUid(inclusion);
        boolean isNewPrecondition = implicationsStore.addInclusionEffect(condition, inclusion);

        if (!isNewPrecondition) {
            return false;
        }

        if (isNew) {
            logger.info("Found new precondition {} for NOVEL point {}", condition, inclusion);
        } else {
            logger.info("Found new precondition {} for existing point {}", condition, inclusion);
        }

        return true;
    }

    public boolean addExclusionForFaultUid(Collection<Behaviour> condition, FaultUid exclusion) {
        boolean isNewPrecondition = implicationsStore.addExclusionEffect(condition, exclusion);

        if (!isNewPrecondition) {
            return false;
        }

        return true;
    }

    public boolean hasFaultUidSubset(Set<FaultUid> set) {
        for (var redundant : this.redundantUidSubsets) {
            if (Sets.isSubsetOf(redundant, set)) {
                return true;
            }
        }

        return false;
    }

    public boolean pruneFaultUidSubset(Set<FaultUid> subset) {
        // If the subset is already in the list of redundant subsets
        // Or if the subset is a subset of an already redundant subset
        // Then we can ignore this subset
        if (hasFaultUidSubset(subset)) {
            return false;
        }

        // filter out all supersets of this subset
        this.redundantUidSubsets.removeIf(s -> Sets.isSubsetOf(subset, s));
        // This is a novel redundant subset, lets add it!
        this.redundantUidSubsets.add(subset);
        return true;
    }

    public boolean hasFaultSubset(Set<Fault> given) {
        for (var redundant : this.redundantFaultSubsets) {
            if (Sets.isSubsetOf(redundant, given)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidSubset(Set<Fault> subset) {
        // Must have at most one fault per faultuid
        Set<FaultUid> uids = new HashSet<>();
        for (var fault : subset) {
            if (!uids.add(fault.uid())) {
                return false;
            }
        }
        return true;
    }

    public boolean pruneFaultSubset(Set<Fault> subset) {
        // If the subset is already in the list of redundant subsets
        // Or if the subset is a subset of an already redundant subset
        // Then we can ignore this subset

        if (!isValidSubset(subset)) {
            throw new IllegalArgumentException("Fault subset must have at most one fault per faultuid");
        }

        if (hasFaultSubset(subset)) {
            return false;
        }

        // TODO: if we have all fault modes for a faultuid in the subsets
        // we can ignore the faultuid

        // filter out all supersets of this subset
        this.redundantFaultSubsets.removeIf(s -> Sets.isSubsetOf(subset, s));
        // and add this subset
        this.redundantFaultSubsets.add(subset);
        return true;
    }

    public boolean pruneFaultload(Faultload faultload) {
        // If the faultload is already in the list of redundant faultloads
        // Then we can ignore this faultload
        if (hasFaultload(faultload)) {
            return false;
        }

        this.redundantFaultloads.add(faultload.faultSet());
        return true;
    }

    public boolean hasFaultload(Faultload faultload) {
        return this.redundantFaultloads.contains(faultload.faultSet());
    }

    public boolean hasFaultload(Set<Fault> faultload) {
        return this.redundantFaultloads.contains(faultload);
    }

    public Set<Behaviour> getExpectedBehaviour(Set<Fault> faults) {
        return implicationsStore.getBehaviours(faults);
    }

    public Set<FaultUid> getExpectedPoints(Set<Fault> faults) {
        return implicationsStore.getBehaviours(faults)
                .stream()
                .map(Behaviour::uid)
                .collect(Collectors.toSet());
    }

    public boolean isRedundant(Set<Fault> faultload) {
        if (faultload == null || faultload.isEmpty()) {
            return false;
        }

        // Prune on subsets
        if (hasFaultSubset(faultload)) {
            logger.debug("Pruning node {} due pruned subset", faultload);
            return true;
        }

        // Prune on faultload
        if (hasFaultload(faultload)) {
            logger.debug("Pruning node {} due pruned faultload", faultload);
            return true;
        }

        // Prune on injecting faults on unreachable points
        var expected = getExpectedBehaviour(faultload);
        if (expected.isEmpty()) {
            return false;
        }

        for (Fault toInject : faultload) {
            boolean found = false;
            for (Behaviour point : expected) {
                if (point.uid().matches(toInject.uid())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                logger.debug("Pruning node {} due to unreachable point {}", faultload, toInject);
                return true;
            }
        }

        return false;
    }

    public long estimatePruned(List<FaultUid> allUids) {
        return estimatePruned(Set.copyOf(allUids));
    }

    public long estimatePruned(Set<FaultUid> allUids) {
        int modeCount = this.modes.size();
        long sum = 0;

        // Note: this does not account for overlap between uid and fault subsets
        long pointSubsetContribution = SpaceEstimate.estimatePointSubsetsImpact(allUids, redundantUidSubsets,
                modeCount);
        sum += pointSubsetContribution;

        long faultSubsetContribution = SpaceEstimate.estimateFaultSubsetsImpact(allUids, redundantFaultSubsets,
                modeCount);
        sum += faultSubsetContribution;

        long faultloadContribution = redundantFaultloads.size();
        sum += faultloadContribution;

        long inclusionContribution = 0;
        sum += inclusionContribution;

        long exclusionContribution = 0;
        sum += exclusionContribution;

        return sum;
    }

    public long estimatePruned() {
        return estimatePruned(getPoints());
    }

}
