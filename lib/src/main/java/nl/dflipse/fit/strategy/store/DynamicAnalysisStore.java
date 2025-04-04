package nl.dflipse.fit.strategy.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.dflipse.fit.faultload.Fault;
import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.faultload.faultmodes.FaultMode;
import nl.dflipse.fit.strategy.util.Sets;
import nl.dflipse.fit.strategy.util.SpaceEstimate;

public class DynamicAnalysisStore {
    private final Logger logger = LoggerFactory.getLogger(DynamicAnalysisStore.class);

    private final Set<FaultMode> modes;
    private final Set<FaultUid> points = new HashSet<>();

    // Stores which faults must be present for a faultuid to be injected
    private final ConditionalStore inclusionConditions = new ConditionalStore();
    // Stores which faults must not be present for a faultuid to be injected
    private final ConditionalStore exclusionConditions = new ConditionalStore();

    private final List<Set<Fault>> redundantFaultloads = new ArrayList<>();
    private final List<Set<FaultUid>> redundantUidSubsets = new ArrayList<>();
    private final List<Set<Fault>> redundantFaultSubsets = new ArrayList<>();

    public DynamicAnalysisStore(Set<FaultMode> modes) {
        this.modes = modes;
    }

    public Set<FaultUid> getFaultInjectionPoints() {
        return points;
    }

    public Set<FaultMode> getModes() {
        return modes;
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

    public Set<FaultUid> getFaultUids() {
        return points;
    }

    public Set<FaultUid> getNonConditionalFaultUids() {
        return points.stream()
                .filter(fid -> !inclusionConditions.hasConditions(fid))
                .collect(Collectors.toSet());
    }

    public ConditionalStore getInclusionConditions() {
        return inclusionConditions;
    }

    public ConditionalStore getExclusionConditions() {
        return exclusionConditions;
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

    public int addFaultUids(List<FaultUid> fids) {
        int added = 0;

        for (var fid : fids) {
            boolean isNew = addFaultUid(fid);
            if (isNew) {
                added++;
            }
        }

        return added;
    }

    public boolean addConditionForFaultUid(Set<Fault> condition, FaultUid fid) {
        boolean isNew = addFaultUid(fid);
        boolean isNewPrecondition = inclusionConditions.addCondition(condition, fid);

        if (!isNewPrecondition) {
            return false;
        }

        if (isNew) {
            logger.info("Found new precondition {} for NOVEL fault {}", condition, fid);
        } else {
            logger.info("Found new precondition {} for existing fault {}", condition, fid);
        }

        return true;
    }

    public boolean addExclusionForFaultUid(Set<Fault> condition, FaultUid fid) {
        boolean isNewPrecondition = exclusionConditions.addCondition(condition, fid);

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
        return estimatePruned(getFaultInjectionPoints());
    }

}
