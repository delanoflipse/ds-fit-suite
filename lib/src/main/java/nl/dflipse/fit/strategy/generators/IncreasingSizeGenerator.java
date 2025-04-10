package nl.dflipse.fit.strategy.generators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.dflipse.fit.faultload.Fault;
import nl.dflipse.fit.faultload.FaultUid;
import nl.dflipse.fit.faultload.Faultload;
import nl.dflipse.fit.faultload.faultmodes.FailureMode;
import nl.dflipse.fit.strategy.Reporter;
import nl.dflipse.fit.strategy.store.DynamicAnalysisStore;
import nl.dflipse.fit.strategy.util.Faults;
import nl.dflipse.fit.strategy.util.PrunableGenericPowersetTreeIterator;
import nl.dflipse.fit.strategy.util.SpaceEstimate;

public class IncreasingSizeGenerator implements Generator, Reporter {
    private final Logger logger = LoggerFactory.getLogger(IncreasingSizeGenerator.class);
    private PrunableGenericPowersetTreeIterator iterator;

    private final DynamicAnalysisStore store;

    public IncreasingSizeGenerator(DynamicAnalysisStore store) {
        this.store = store;
    }

    public IncreasingSizeGenerator(List<FailureMode> modes) {
        this.store = new DynamicAnalysisStore(modes);
    }

    @Override
    public void reportFaultUids(List<FaultUid> potentialFaults) {
        if (potentialFaults == null || potentialFaults.isEmpty()) {
            return;
        }

        int modeCount = getFailureModes().size();

        if (iterator == null) {
            store.addFaultUids(potentialFaults);
            iterator = new PrunableGenericPowersetTreeIterator(store, true);

            long expectedSize = iterator.size(modeCount);
            logger.info("Found " + potentialFaults.size() + " fault points. Will generate at most "
                    + expectedSize
                    + " new test cases");
        } else {
            long oldSize = iterator.size(modeCount);

            for (var fid : potentialFaults) {
                boolean isNew = store.addFaultUid(fid);
                if (!isNew) {
                    continue;
                }

                logger.info("Found NEW fault injection point: " + fid);
                iterator.add(fid);
            }

            long newSize = iterator.size(modeCount) - oldSize;
            logger.info("Added " + newSize + " new test cases");
        }
    }

    @Override
    public void reportPreconditionOfFaultUid(Set<Fault> condition, FaultUid fid) {
        if (fid == null) {
            return;
        }

        if (condition.isEmpty()) {
            reportFaultUids(List.of(fid));
            return;
        }

        if (iterator == null) {
            throw new IllegalStateException(
                    "Cannot add conditional fault injection point if no normal fault injection points are discovered");
        }

        int m = getFailureModes().size();
        long oldSize = iterator.size(m);
        boolean isNew = store.addConditionForFaultUid(condition, fid);

        if (!isNew) {
            return;
        }

        iterator.pruneQueue();
        iterator.addConditional(condition, fid);

        long newSize = iterator.size(m) - oldSize;
        logger.info("Added " + newSize + " new test cases");
    }

    @Override
    public void reportExclusionOfFaultUid(Set<Fault> condition, FaultUid fid) {
        if (fid == null) {
            return;
        }

        if (condition.isEmpty()) {
            throw new IllegalArgumentException("Exclusion must be non-empty");
        }

        if (iterator == null) {
            throw new IllegalStateException(
                    "Cannot exlude fault injection point if no normal fault injection points are discovered");
        }

        boolean isNew = store.addExclusionForFaultUid(condition, fid);

        if (!isNew) {
            return;
        }

        iterator.pruneQueue();
    }

    @Override
    public Faultload generate() {
        // If we have exhausted the mode-faultUid pairings
        // we need to get the next fault injection space point (subset of fault
        // injection points)
        if (iterator == null || !iterator.hasNext()) {
            return null;
        }

        // create next faultload
        var nextFaults = iterator.next();
        Faultload faultLoad = new Faultload(nextFaults);

        // if we should skip this specific faultload
        // then we should generate the next one
        if (store.hasFaultload(faultLoad)) {
            return generate();
        }

        return faultLoad;
    }

    @Override
    public void pruneFaultUidSubset(Set<FaultUid> subset) {
        boolean isNew = false;
        var allFaults = Faults.allFaults(subset, getFailureModes());
        for (Set<Fault> prunedSet : allFaults) {
            boolean wasNew = store.pruneFaultSubset(prunedSet);
            isNew = isNew || wasNew;
        }

        if (isNew && iterator != null) {
            iterator.pruneQueue();
        }
    }

    private int getNumerOfPoints() {
        return store.getFaultInjectionPoints().size();
    }

    @Override
    public void pruneFaultSubset(Set<Fault> subset) {
        boolean isNew = store.pruneFaultSubset(subset);
        if (isNew && iterator != null) {
            iterator.pruneQueue();
        }
    }

    @Override
    public void pruneFaultload(Faultload faultload) {
        boolean isNew = store.pruneFaultload(faultload);
        if (isNew && iterator != null) {
            iterator.pruneQueue();
        }
    }

    @Override
    public long spaceSize() {
        return SpaceEstimate.spaceSize(getFailureModes().size(), getNumerOfPoints());
    }

    @Override
    public List<FailureMode> getFailureModes() {
        return store.getModes();
    }

    @Override
    public List<FaultUid> getFaultInjectionPoints() {
        return store.getFaultInjectionPoints();
    }

    @Override
    public Set<FaultUid> getExpectedPoints(Set<Fault> faultload) {
        return store.getExpectedPoints(faultload);
    }

    @Override
    public Set<FaultUid> getExpectedPoints(Faultload faultload) {
        return getExpectedPoints(faultload.faultSet());
    }

    public DynamicAnalysisStore getStore() {
        return store;
    }

    public int getMaxQueueSize() {
        return iterator == null ? 0 : iterator.getMaxQueueSize();
    }

    public int getQueuSize() {
        return iterator == null ? 0 : iterator.getQueuSize();
    }

    public long getSpaceLeft() {
        return iterator == null ? 0 : iterator.size(getFailureModes().size());
    }

    @Override
    public Map<String, String> report() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("Fault injection points", String.valueOf(getNumerOfPoints()));
        report.put("Modes", String.valueOf(getFailureModes().size()));
        report.put("Redundant faultloads", String.valueOf(store.getRedundantFaultloads().size()));
        report.put("Redundant fault points", String.valueOf(store.getRedundantUidSubsets().size()));
        report.put("Redundant fault subsets", String.valueOf(store.getRedundantFaultSubsets().size()));
        report.put("Max queue size", String.valueOf(getMaxQueueSize()));
        int queueSize = getQueuSize();
        if (queueSize > 0) {
            report.put("Queue size (left)", String.valueOf(getQueuSize()));
            report.put("Space left", String.valueOf(getSpaceLeft()));
        }

        int i = 0;
        for (var point : getFaultInjectionPoints()) {
            report.put("FID(" + i + ")", point.toString());
            i++;
        }

        return report;
    }

}
